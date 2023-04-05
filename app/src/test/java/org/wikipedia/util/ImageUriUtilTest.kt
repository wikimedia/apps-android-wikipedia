package org.wikipedia.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test

class ImageUriUtilTest {
    @Test
    fun testUrlForSizeStringWithLargeSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/200px-PaeoniaSuffruticosa7.jpg", 1280),
            Matchers.`is`("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/1280px-PaeoniaSuffruticosa7.jpg"))
    }

    @Test
    fun testUrlForSizeStringWithSmallSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/1280px-PaeoniaSuffruticosa7.jpg", 200),
            Matchers.`is`("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/200px-PaeoniaSuffruticosa7.jpg"))
    }

    @Test
    fun testUrlForSizeStringWithSmallerSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/320px-PaeoniaSuffruticosa7.jpg", 200),
            Matchers.`is`("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/200px-PaeoniaSuffruticosa7.jpg"))
    }

    @Test
    fun testUrlForPreferredSizeWithNumericName() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/320px-Paeonia_californica_2320679478.jpg", 1280),
            Matchers.`is`("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/1280px-Paeonia_californica_2320679478.jpg"))
    }

    @Test
    fun testUrlForPagedSizeStringWithLargeSize() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize("https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/Dehnungsmethoden_DrKlee.pdf/page1-320px-Dehnungsmethoden_DrKlee.pdf.jpg", 1280),
            Matchers.`is`("https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/Dehnungsmethoden_DrKlee.pdf/page1-1280px-Dehnungsmethoden_DrKlee.pdf.jpg"))
    }

    @Test
    fun testUrlForPagedSizeStringWithPxInName() {
        MatcherAssert.assertThat(ImageUrlUtil.getUrlForPreferredSize("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Lossy-page1-2658px-Nelson%27s_Pillar%2C_Sackville-Street%2C_Dublin_RMG_PU3914_%28cropped%29.jpg/492px-Lossy-page1-2658px-Nelson%27s_Pillar%2C_Sackville-Street%2C_Dublin_RMG_PU3914_%28cropped%29.jpg", 640),
            Matchers.`is`("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Lossy-page1-2658px-Nelson%27s_Pillar%2C_Sackville-Street%2C_Dublin_RMG_PU3914_%28cropped%29.jpg/640px-Lossy-page1-2658px-Nelson%27s_Pillar%2C_Sackville-Street%2C_Dublin_RMG_PU3914_%28cropped%29.jpg"))
    }
}
