package org.wikipedia.search

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.json.GsonUtil

class SearchResultsRedirectProcessingTest {
    private lateinit var result: MwQueryResult

    @Before
    fun setUp() {
        result = GsonUtil.getDefaultGson().fromJson(queryJson, MwQueryResult::class.java)
    }

    @Test
    fun testRedirectHandling() {
        val pages = result.pages!!
        MatcherAssert.assertThat(pages.size, Matchers.`is`(2))
        MatcherAssert.assertThat(pages[0].title, Matchers.`is`("Narthecium#Foo"))
        MatcherAssert.assertThat(pages[0].redirectFrom, Matchers.`is`("Abama"))
        MatcherAssert.assertThat(pages[1].title, Matchers.`is`("Amitriptyline"))
        MatcherAssert.assertThat(pages[1].redirectFrom, Matchers.`is`("Abamax"))
    }

    @Test
    fun testConvertTitleHandling() {
        val pages = result.pages!!
        MatcherAssert.assertThat(pages.size, Matchers.`is`(2))
        MatcherAssert.assertThat(pages[0].title, Matchers.`is`("Narthecium#Foo"))
        MatcherAssert.assertThat(pages[0].convertedFrom, Matchers.`is`("NotNarthecium"))
    }

    private val queryJson = """{
    "converted": [
      {
        "from": "NotNarthecium",
        "to": "Narthecium"
      }
    ],
    "redirects": [
      {
        "index": 1,
        "from": "Abama",
        "to": "Narthecium",
        "tofragment": "Foo"
      },
      {
        "index": 2,
        "from": "Abamax",
        "to": "Amitriptyline"
      }
    ],
    "pages":[
      {
        "pageid": 2060913,
        "ns": 0,
        "title": "Narthecium",
        "index": 1,
        "terms": {
          "description": [
            "genus of plants"
          ]
        },
        "thumbnail": {
          "source": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Narthecium_ossifragum_01.jpg/240px-Narthecium_ossifragum_01.jpg",
          "width": 240,
          "height": 320
        }
      },
      {
        "pageid": 583678,
        "ns": 0,
        "title": "Amitriptyline",
        "index": 2,
        "terms": {
          "description": [
            "chemical compound",
            "chemical compound"
          ]
        },
        "thumbnail": {
          "source": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Amitriptyline2DACS.svg/318px-Amitriptyline2DACS.svg.png",
          "width": 318,
          "height": 320
        }
      }
    ]
  }"""
}
