package org.wikipedia.search.interleave

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitMix64Test {
    @Test
    fun testOutputsMatchSharedVectors() {
        TeamDraftMergeVectors.load().splitmix64.forEach { vector ->
            val random = SplitMix64(vector.seed.toULong())
            val outputs = vector.outputs.map { random.nextULong().toString() }
            assertEquals("seed ${vector.seed}", vector.outputs, outputs)
        }
    }

    @Test
    fun testCoinIsTopBitOfNextOutput() {
        val outputs = SplitMix64(1234567uL)
        val coins = SplitMix64(1234567uL)
        repeat(64) {
            assertEquals((outputs.nextULong() shr 63).toInt(), coins.nextCoin())
        }
    }
}
