package org.wikipedia.search.interleave

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil

@Serializable
class TeamDraftMergeVectors(
    @SerialName("merge_version") val mergeVersion: Int,
    val splitmix64: List<SplitMix64Vector>,
    val vectors: List<MergeVector>
) {
    @Serializable
    class SplitMix64Vector(
        val seed: String,
        val outputs: List<String>
    )

    @Serializable
    class MergeVector(
        val name: String,
        val seed: String,
        @SerialName("list_length") val listLength: Int,
        val lexical: List<Long>,
        val semantic: List<Long>,
        val expected: ExpectedResult
    )

    @Serializable
    class ExpectedResult(
        @SerialName("coin_flips") val coinFlips: String,
        val slots: List<ExpectedSlot>
    )

    @Serializable
    data class ExpectedSlot(
        @SerialName("page_id") val pageId: Long,
        val team: String,
        val rank: Int,
        @SerialName("also_in_other") val alsoInOther: Boolean
    )

    companion object {
        fun load(): TeamDraftMergeVectors {
            return JsonUtil.decodeFromString(TestFileUtil.readRawFile("team_draft_merge_vectors.json"))!!
        }
    }
}
