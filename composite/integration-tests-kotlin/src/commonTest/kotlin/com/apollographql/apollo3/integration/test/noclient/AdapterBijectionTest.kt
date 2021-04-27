package com.apollographql.apollo3.integration.test.noclient

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.LocalDateResponseAdapter
import com.apollographql.apollo3.integration.httpcache.type.Types
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroWithDatesQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroWithInlineFragmentQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsWithFragmentsQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameWithEnumsQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo3.integration.normalizer.fragment.HeroWithFriendsFragmentImpl
import com.apollographql.apollo3.integration.normalizer.type.Episode
import kotlinx.datetime.LocalDate
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that test that transforming a model and parsing it back gives the same model.
 * They test 2 scenarios:
 *
 * model -> json -> model
 * model -> map -> records -> model
 */
class AdapterBijectionTest {

  private val responseAdapterCache = ResponseAdapterCache(mapOf(Types.Date.name to LocalDateResponseAdapter))

  @Test
  fun customScalar1() = bijection(
      EpisodeHeroWithDatesQuery(),
      EpisodeHeroWithDatesQuery.Data(
          EpisodeHeroWithDatesQuery.Data.Hero(
              "R222-D222",
              LocalDate(1985, 4, 16),
              emptyList()
          )
      )
  )

  @Test
  fun customScalar2() = bijection(
      EpisodeHeroWithDatesQuery(),
      EpisodeHeroWithDatesQuery.Data(
          EpisodeHeroWithDatesQuery.Data.Hero(
              "R22-D22",
              LocalDate(1986, 4, 16),
              listOf(
                  LocalDate(2017, 4, 16),
                  LocalDate(2017, 5, 16),
              )
          )
      )
  )

  @Test
  fun enum1() = bijection(
      HeroNameWithEnumsQuery(),
      HeroNameWithEnumsQuery.Data(
          HeroNameWithEnumsQuery.Data.Hero(
              "R222-D222",
              Episode.JEDI, emptyList<Episode>()
          )
      )
  )

  @Test
  fun enum2() = bijection(
      HeroNameWithEnumsQuery(),
      HeroNameWithEnumsQuery.Data(
          HeroNameWithEnumsQuery.Data.Hero(
              "R22-D22",
              Episode.JEDI,
              listOf(Episode.EMPIRE)
          )
      )
  )

  @Test
  fun objects1() = bijection(
      HeroAndFriendsNamesWithIDsQuery(),
      HeroAndFriendsNamesWithIDsQuery.Data(
          HeroAndFriendsNamesWithIDsQuery.Data.Hero(
              "2001",
              "R222-D222",
              null
          )
      )
  )

  @Test
  fun objects2() = bijection(
      HeroAndFriendsNamesWithIDsQuery(),
      HeroAndFriendsNamesWithIDsQuery.Data(
          HeroAndFriendsNamesWithIDsQuery.Data.Hero(
              "2001",
              "R222-D222",
              listOf(
                  HeroAndFriendsNamesWithIDsQuery.Data.Hero.Friend(
                      "1002",
                      "Han Soloooo"
                  )
              )
          )
      )
  )


  @Test
  fun namedFragments() = bijection(
      HeroAndFriendsWithFragmentsQuery(),
      HeroAndFriendsWithFragmentsQuery.Data(
          HeroAndFriendsWithFragmentsQuery.Data.Hero(
              __typename = "Droid",
              id = "2001",
              name = "R222-D222",
              friends = listOf(
                  HeroAndFriendsWithFragmentsQuery.Data.Hero.HumanFriend(
                      __typename = "Human",
                      id = "1006",
                      name = "SuperMan"
                  ),
                  HeroAndFriendsWithFragmentsQuery.Data.Hero.HumanFriend(
                      __typename = "Human",
                      id = "1004",
                      name = "Beast"
                  )
              )
          )
      )
  )

  @Test
  fun inlineFragments() = bijection(
      EpisodeHeroWithInlineFragmentQuery(),
      EpisodeHeroWithInlineFragmentQuery.Data(
          EpisodeHeroWithInlineFragmentQuery.Data.Hero(
              name = "R22-D22",
              friends = listOf(
                  EpisodeHeroWithInlineFragmentQuery.Data.Hero.HumanFriend(
                      __typename = "Human",
                      id = "1002",
                      name = "Han Solo",
                      height = 2.5
                  ),
                  EpisodeHeroWithInlineFragmentQuery.Data.Hero.DroidFriend(
                      __typename = "Droid",
                      primaryFunction = "Entertainment",
                      name = "RD",
                  ),
              )
          )
      )
  )

  @Test
  fun listOfList() = bijection(
      StarshipByIdQuery("Starship1"),
      StarshipByIdQuery.Data(
          StarshipByIdQuery.Data.Starship(
              "Starship1",
              "SuperRocket",
              listOf(
                  listOf(900.0, 800.0),
                  listOf(700.0, 600.0)
              )
          )
      )
  )

  /**
   * Fixme: add Fragment.fromResponse() and toJson()
   */
//  @Test
//  fun fragmentImplementation() = bijection(
//      HeroWithFriendsFragmentImpl(),
//      HeroWithFriendsFragmentImpl.Data(
//          __typename = "Droid",
//          id = "2001",
//          name = "R222-D222",
//          friends = listOf(
//              HeroWithFriendsFragmentImpl.Data.HumanFriend(
//                  __typename = "Human",
//                  id = "1000",
//                  name = "SuperMan"
//              ),
//              HeroWithFriendsFragmentImpl.Data.HumanFriend(
//                  __typename = "Human",
//                  id = "1002",
//                  name = "Han Solo"
//              ),
//          )
//      )
//  )

  private fun <D : Operation.Data> bijection(operation: Operation<D>, data: D) {
    val json = operation.toJson(data = data, responseAdapterCache = responseAdapterCache)
    val data2 = operation.fromResponse(Buffer().apply { writeUtf8(json) }, responseAdapterCache).data

    assertEquals(data, data2)
  }
}