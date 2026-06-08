package org.wikipedia.gallery

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.test.MockRetrofitTest

class ImageLicenseFetchClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("image_license.json")
        runBlocking {
            getImageInfo()
        }.run {
            val imageInfo = query?.pages?.firstOrNull()?.imageInfo()
            val imageLicense = imageInfo?.metadata?.let { ImageLicense(it) } ?: ImageLicense()
            assertEquals("cc-by-sa-4.0", imageLicense.licenseName)
            assertEquals("CC BY-SA 4.0", imageLicense.licenseShortName)
            assertEquals("http://creativecommons.org/licenses/by-sa/4.0", imageLicense.licenseUrl)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                getImageInfo()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        runBlocking {
            try {
                getImageInfo()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        runBlocking {
            try {
                getImageInfo()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    private suspend fun getImageInfo(): MwQueryResponse {
        return apiService.getImageInfo(PAGE_TITLE_MARK_SELBY.prefixedText, WIKISITE_TEST.languageCode)
    }

    companion object {
        private val WIKISITE_TEST = forLanguageCode("test")
        private val PAGE_TITLE_MARK_SELBY = PageTitle("File:Mark_Selby_at_Snooker_German_Masters_(DerHexer)_2015-02-04_02.jpg", WIKISITE_TEST)
    }
}
