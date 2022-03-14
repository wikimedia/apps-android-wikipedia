package org.wikipedia.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test

class ImageUriUtilTest {
    @Test
    fun testUrlForSizeStringWithLargeSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_320, IMAGE_SIZE_1280), Matchers.`is`(IMAGE_URL_1280))
    }

    @Test
    fun testUrlForSizeStringWithSmallSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_1280, IMAGE_SIZE_200), Matchers.`is`(IMAGE_URL_200))
    }

    @Test
    fun testUrlForSizeStringWithSmallerSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_320, IMAGE_SIZE_200), Matchers.`is`(IMAGE_URL_200))
    }

    @Test
    fun testUrlForPreferredSizeWithNumericName() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_WITH_NUMERIC_NAME_320, IMAGE_SIZE_1280),
            Matchers.`is`(IMAGE_URL_WITH_NUMERIC_NAME_1280))
    }

    companion object {
        private const val IMAGE_SIZE_1280 = 1280
        private const val IMAGE_SIZE_200 = 200
        private const val IMAGE_URL_200 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/200px-PaeoniaSuffruticosa7.jpg"
        private const val IMAGE_URL_320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/320px-PaeoniaSuffruticosa7.jpg"
        private const val IMAGE_URL_1280 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/1280px-PaeoniaSuffruticosa7.jpg"
        private const val IMAGE_URL_WITH_NUMERIC_NAME_320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/320px-Paeonia_californica_2320679478.jpg"
        private const val IMAGE_URL_WITH_NUMERIC_NAME_1280 = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/1280px-Paeonia_californica_2320679478.jpg"
    }
}
