package org.wikipedia.pageimages

import io.reactivex.rxjava3.core.Observable
import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.test.MockRetrofitTest

class PageImagesClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("reading_list_page_info.json")
        val titles = mutableListOf<PageTitle>()
        titles.add(PAGE_TITLE_OBAMA)
        titles.add(PAGE_TITLE_BIDEN)
        getObservable(titles).test().await()
            .assertComplete().assertNoErrors()
            .assertValue {
                val biden = it[PAGE_TITLE_BIDEN]
                val obama = it[PAGE_TITLE_OBAMA]
                biden!!.apiTitle == "Joe_Biden" && biden.imageName == "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Official_portrait_of_Vice_President_Joe_Biden.jpg/255px-Official_portrait_of_Vice_President_Joe_Biden.jpg" && obama!!.apiTitle == "Barack_Obama" && obama.imageName == "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        getObservable(emptyList()).test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        getObservable(emptyList()).test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        getObservable(emptyList()).test().await()
            .assertError(Exception::class.java)
    }

    private fun getObservable(titles: List<PageTitle>): Observable<Map<PageTitle?, PageImage>> {
        return apiService.getPageImages("foo")
            .map {
                imageMapFromPages(WIKISITE_TEST, titles, it.query!!.pages!!)
            }
    }

    companion object {
        private val WIKISITE_TEST = WikiSite.forLanguageCode("test")
        private val PAGE_TITLE_BIDEN = PageTitle("Joe Biden", WIKISITE_TEST)
        private val PAGE_TITLE_OBAMA = PageTitle("Barack Obama", WIKISITE_TEST)

        private fun imageMapFromPages(wiki: WikiSite, titles: List<PageTitle>, pages: List<MwQueryPage>): Map<PageTitle?, PageImage> {
            val pageImagesMap = mutableMapOf<PageTitle?, PageImage>()
            // nominal case
            val titlesMap = mutableMapOf<String, PageTitle>()
            for (title in titles) {
                titlesMap[title.prefixedText] = title
            }
            val thumbnailSourcesMap = mutableMapOf<String, String?>()

            // noinspection ConstantConditions
            for (page in pages) {
                thumbnailSourcesMap[PageTitle(null, page.title, wiki).prefixedText] = page.thumbUrl()
                if (!page.convertedFrom.isNullOrEmpty()) {
                    val pageTitle = PageTitle(null, page.convertedFrom!!, wiki)
                    thumbnailSourcesMap[pageTitle.prefixedText] = page.thumbUrl()
                }
                if (!page.redirectFrom.isNullOrEmpty()) {
                    thumbnailSourcesMap[PageTitle(null, page.redirectFrom!!, wiki).prefixedText] = page.thumbUrl()
                }
            }
            for ((key, title) in titlesMap) {
                if (thumbnailSourcesMap.containsKey(key)) {
                    pageImagesMap[title] = PageImage(title, thumbnailSourcesMap[key])
                }
            }
            return pageImagesMap
        }
    }
}
