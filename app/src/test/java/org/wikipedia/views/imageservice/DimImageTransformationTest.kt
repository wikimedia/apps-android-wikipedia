package org.wikipedia.views.imageservice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DimImageTransformationTest {

    @Test
    fun testCacheKeyIncludesDimmingState() {
        assertNotEquals(
            DimImageTransformation(dimImages = true).cacheKey,
            DimImageTransformation(dimImages = false).cacheKey
        )
    }

    @Test
    fun testCacheKeyFormat() {
        assertEquals("DimImageTransformation:true", DimImageTransformation(dimImages = true).cacheKey)
        assertEquals("DimImageTransformation:false", DimImageTransformation(dimImages = false).cacheKey)
    }
}
