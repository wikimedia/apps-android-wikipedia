package org.wikipedia.search.interleave

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil

@Serializable
class LexicalListVectors(
    @SerialName("lexical_version") val lexicalVersion: Int,
    val vectors: List<LexicalListVector>
) {
    @Serializable
    class LexicalListVector(
        val name: String,
        @SerialName("full_text_threshold") val fullTextThreshold: Int,
        val prefix: List<Int>,
        @SerialName("full_text") val fullText: List<Int>?,
        val expected: List<Int>
    )

    companion object {
        fun load(): LexicalListVectors {
            return JsonUtil.decodeFromString(TestFileUtil.readRawFile("lexical_list_vectors.json"))!!
        }
    }
}
