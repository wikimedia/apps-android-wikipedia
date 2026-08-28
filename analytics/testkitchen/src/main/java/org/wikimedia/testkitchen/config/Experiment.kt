@file:UseSerializers(InstantSerializer::class)

package org.wikimedia.testkitchen.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.wikimedia.testkitchen.context.InstantSerializer
import java.time.Instant

@Serializable
class Experiment : Sampleable() {
    val slug: String = ""
    @SerialName("utc_start_dt") val start: Instant? = null
    @SerialName("utc_end_dt") val end: Instant? = null
    val status: Int = 0
    val groups: List<TestGroup> = emptyList()

    @Serializable
    class TestGroup {
        val name: String = ""
        val slug: String = ""
        val description: String = ""
    }
}
