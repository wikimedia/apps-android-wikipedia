package org.wikipedia.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class ImageUrlUtilTest {

    @Test
    fun `isGif returns true for URLS ending with gif extension`() {
        assertTrue(ImageUrlUtil.isGif("https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif"))
        assertTrue(ImageUrlUtil.isGif("https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.Gif"))
        assertTrue(ImageUrlUtil.isGif("https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.GIF"))
    }

    @Test
    fun `isGif returns false for non-gif URLS`() {
        assertFalse(ImageUrlUtil.isGif("https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a7/How_to_make_video.webm/How_to_make_video.webm.720p.vp9.webm"))
        assertFalse(ImageUrlUtil.isGif("https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a7/How_to_make_video.webm/How_to_make_video.webm.720p.vp9.mp4"))
    }

    @Test
    fun `isGif return false for invalid URLS`() {
        assertFalse(ImageUrlUtil.isGif("google.com"))
        assertFalse(ImageUrlUtil.isGif("test.gif"))
        assertFalse(ImageUrlUtil.isGif(null))
    }
}
