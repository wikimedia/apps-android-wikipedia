package org.wikipedia.gallery

import org.junit.Test
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.page.PageTitle
import org.wikipedia.test.MockRetrofitTest

class ImageLicenseFetchClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("image_license.json")
        apiService.getImageInfo(PAGE_TITLE_MARK_SELBY.prefixedText, WIKISITE_TEST.languageCode)
            .map { response ->
                // noinspection ConstantConditions
                val imageInfo = response.query?.pages?.firstOrNull()?.imageInfo()
                if (imageInfo?.metadata != null) ImageLicense(imageInfo.metadata!!) else ImageLicense()
            }
            .test().await()
            .assertComplete().assertNoErrors()
            .assertValue { result -> result.licenseName == "cc-by-sa-4.0" && result.licenseShortName == "CC BY-SA 4.0" && result.licenseUrl == "http://creativecommons.org/licenses/by-sa/4.0" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        apiService.getImageInfo(PAGE_TITLE_MARK_SELBY.prefixedText, WIKISITE_TEST.languageCode)
            .map { ImageLicense() }
            .test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        apiService.getImageInfo(PAGE_TITLE_MARK_SELBY.prefixedText, WIKISITE_TEST.languageCode)
            .map { ImageLicense() }
            .test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        apiService.getImageInfo(PAGE_TITLE_MARK_SELBY.prefixedText, WIKISITE_TEST.languageCode)
            .map { ImageLicense() }
            .test().await()
            .assertError(Exception::class.java)
    }

    companion object {
        private val WIKISITE_TEST = forLanguageCode("test")
        private val PAGE_TITLE_MARK_SELBY = PageTitle("File:Mark_Selby_at_Snooker_German_Masters_(DerHexer)_2015-02-04_02.jpg", WIKISITE_TEST)
    }
}
