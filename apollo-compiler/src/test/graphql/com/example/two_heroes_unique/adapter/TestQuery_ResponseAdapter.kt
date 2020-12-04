// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.two_heroes_unique.adapter

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import com.example.two_heroes_unique.TestQuery
import kotlin.Array
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
object TestQuery_ResponseAdapter : ResponseAdapter<TestQuery.Data> {
  private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField.forObject("r2", "hero", null, true, null),
    ResponseField.forObject("luke", "hero", mapOf<String, Any>(
      "episode" to "EMPIRE"), true, null)
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
    return Data.fromResponse(reader, __typename)
  }

  override fun toResponse(writer: ResponseWriter, value: TestQuery.Data) {
    Data.toResponse(writer, value)
  }

  object Data : ResponseAdapter<TestQuery.Data> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forObject("r2", "hero", null, true, null),
      ResponseField.forObject("luke", "hero", mapOf<String, Any>(
        "episode" to "EMPIRE"), true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
      return reader.run {
        var r2: TestQuery.Data.R2? = null
        var luke: TestQuery.Data.Luke? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> r2 = readObject<TestQuery.Data.R2>(RESPONSE_FIELDS[0]) { reader ->
              R2.fromResponse(reader)
            }
            1 -> luke = readObject<TestQuery.Data.Luke>(RESPONSE_FIELDS[1]) { reader ->
              Luke.fromResponse(reader)
            }
            else -> break
          }
        }
        TestQuery.Data(
          r2 = r2,
          luke = luke
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Data) {
      if(value.r2 == null) {
        writer.writeObject(RESPONSE_FIELDS[0], null)
      } else {
        writer.writeObject(RESPONSE_FIELDS[0]) { writer ->
          R2.toResponse(writer, value.r2)
        }
      }
      if(value.luke == null) {
        writer.writeObject(RESPONSE_FIELDS[1], null)
      } else {
        writer.writeObject(RESPONSE_FIELDS[1]) { writer ->
          Luke.toResponse(writer, value.luke)
        }
      }
    }

    object R2 : ResponseAdapter<TestQuery.Data.R2> {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("name", "name", null, false, null)
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data.R2 {
        return reader.run {
          var name: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> name = readString(RESPONSE_FIELDS[0])
              else -> break
            }
          }
          TestQuery.Data.R2(
            name = name!!
          )
        }
      }

      override fun toResponse(writer: ResponseWriter, value: TestQuery.Data.R2) {
        writer.writeString(RESPONSE_FIELDS[0], value.name)
      }
    }

    object Luke : ResponseAdapter<TestQuery.Data.Luke> {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("id", "id", null, false, null),
        ResponseField.forString("name", "name", null, false, null)
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data.Luke {
        return reader.run {
          var id: String? = null
          var name: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> id = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              else -> break
            }
          }
          TestQuery.Data.Luke(
            id = id!!,
            name = name!!
          )
        }
      }

      override fun toResponse(writer: ResponseWriter, value: TestQuery.Data.Luke) {
        writer.writeString(RESPONSE_FIELDS[0], value.id)
        writer.writeString(RESPONSE_FIELDS[1], value.name)
      }
    }
  }
}