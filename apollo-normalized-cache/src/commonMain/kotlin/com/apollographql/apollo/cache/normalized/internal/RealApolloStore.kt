package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.benasher44.uuid.Uuid
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

class RealApolloStore(
    normalizedCache: NormalizedCache,
    private val cacheKeyResolver: CacheKeyResolver,
    val customScalarAdapters: CustomScalarAdapters,
    val logger: ApolloLogger
) : ApolloStore, ReadableStore, WriteableStore {
  private val transactionLock = ReentrantReadWriteLock()
  private val optimisticCache = OptimisticCache().chain(normalizedCache) as OptimisticCache

  private val subscribersLock = reentrantLock()
  private val subscribers = mutableSetOf<ApolloStore.RecordChangeSubscriber>()

  override fun subscribe(subscriber: ApolloStore.RecordChangeSubscriber) {
    subscribersLock.withLock {
      subscribers.add(subscriber)
    }
  }

  override fun unsubscribe(subscriber: ApolloStore.RecordChangeSubscriber) {
    subscribersLock.withLock {
      subscribers.remove(subscriber)
    }
  }

  override fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }

    val subscribers = subscribersLock.withLock {
      subscribers.toList()
    }

    subscribers.forEach { subscriber ->
      subscriber.onCacheRecordsChanged(keys)
    }
  }

  override fun clearAll(): Boolean {
    writeTransaction {
      optimisticCache.clearAll()
    }
    return true
  }

  override fun remove(
      cacheKey: CacheKey,
      cascade: Boolean
  ): Boolean {
    return writeTransaction {
      optimisticCache.remove(cacheKey, cascade)
    }
  }

  override fun remove(
      cacheKeys: List<CacheKey>,
      cascade: Boolean
  ): Int {
    return writeTransaction {
      var count = 0
      for (cacheKey in cacheKeys) {
        if (optimisticCache.remove(cacheKey, cascade = cascade)) {
          count++
        }
      }
      count
    }
  }

  /**
   * not private because tests use it
   */
  fun <R> readTransaction(block: (ReadableStore) -> R): R {
    return transactionLock.read {
      block(this@RealApolloStore)
    }
  }

  /**
   * not private because tests use it
   */
  fun <R> writeTransaction(block: (WriteableStore) -> R): R {
    return transactionLock.write {
      block(this@RealApolloStore)
    }
  }

  override fun normalizedCache(): NormalizedCache {
    return optimisticCache
  }

  override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
    return optimisticCache.loadRecord(key, cacheHeaders)
  }

  override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return optimisticCache.loadRecords(keys, cacheHeaders)
  }

  override fun merge(recordCollection: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(recordCollection, cacheHeaders)
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(record, cacheHeaders)
  }

  override fun cacheKeyResolver(): CacheKeyResolver {
    return cacheKeyResolver
  }

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      cacheHeaders: CacheHeaders
  ): D? {
    return readTransaction { cache ->
      try {
        operation.readDataFromCache(
            customScalarAdapters = customScalarAdapters,
            readableStore = cache,
            cacheKeyResolver = cacheKeyResolver(),
            cacheHeaders = cacheHeaders
        )
      } catch (e: Exception) {
        logger.e(e, "Failed to read cache response")
        null
      }
    }
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      cacheHeaders: CacheHeaders
  ): D? {
    return readTransaction { cache ->
      try {
        fragment.readDataFromCache(
            customScalarAdapters = customScalarAdapters,
            readableStore = cache,
            cacheKeyResolver = cacheKeyResolver(),
            cacheHeaders = cacheHeaders,
            cacheKey = cacheKey
        )
      } catch (e: Exception) {
        logger.e(e, "Failed to read cache response")
        null
      }
    }
  }

  override fun <D : Operation.Data> writeOperationWithRecords(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean
  ): Pair<Set<Record>, Set<String>> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyResolver = cacheKeyResolver
    )

    val changedKeys = optimisticCache.merge(records, cacheHeaders)

    if (publish) {
      publish(changedKeys)
    }

    return records to changedKeys
  }

  override fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean
  ): Set<String> {
    return writeOperationWithRecords(
        operation = operation,
        operationData = operationData,
        cacheHeaders = cacheHeaders,
        publish = publish,
    ).second
  }

  override fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean
  ): Set<String> {
    require(cacheKey != CacheKey.NO_KEY) {
      "ApolloGraphQL: writing a fragment requires a valid cache key"
    }

    return writeTransaction {
      val records = fragment.normalize(
          data = fragmentData,
          customScalarAdapters = customScalarAdapters,
          cacheKeyResolver = cacheKeyResolver,
          rootKey = cacheKey.key
      )

      val changedKeys = merge(records, cacheHeaders)
      if (publish) {
        publish(changedKeys)
      }

      changedKeys
    }
  }

  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>, operationData: D,
      mutationId: Uuid,
      publish: Boolean
  ): Set<String> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyResolver = cacheKeyResolver
    ).map {
      it.toBuilder().mutationId(mutationId).build()
    }

    /**
     * TODO: should we forward the cache headers to the optimistic store?
     */
    val changedKeys = optimisticCache.mergeOptimisticUpdates(records)
    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean
  ): Set<String> {
    val changedKeys = writeTransaction {
      optimisticCache.removeOptimisticUpdates(mutationId)
    }

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }
}