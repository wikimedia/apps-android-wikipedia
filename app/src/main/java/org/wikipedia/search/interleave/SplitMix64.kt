package org.wikipedia.search.interleave

class SplitMix64(seed: ULong) {
    private var state = seed

    fun nextULong(): ULong {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z shr 30)) * MIX_MULTIPLIER_1
        z = (z xor (z shr 27)) * MIX_MULTIPLIER_2
        return z xor (z shr 31)
    }

    fun nextCoin(): Int {
        return (nextULong() shr 63).toInt()
    }

    companion object {
        private const val GOLDEN_GAMMA = 0x9E3779B97F4A7C15uL
        private const val MIX_MULTIPLIER_1 = 0xBF58476D1CE4E5B9uL
        private const val MIX_MULTIPLIER_2 = 0x94D049BB133111EBuL
    }
}
