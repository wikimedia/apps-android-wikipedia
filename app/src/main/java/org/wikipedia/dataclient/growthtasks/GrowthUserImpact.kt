package org.wikipedia.dataclient.growthtasks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.json.JsonUtil

@Suppress("unused")
@Serializable
class GrowthUserImpact(
    @SerialName("@version") val version: Int = 0,
    val userId: Int = 0,
    val userName: String = "",
    val receivedThanksCount: Int = 0,
    val givenThanksCount: Int = 0,
    @SerialName("editCountByNamespace") private val mEditCountByNamespace: JsonElement? = null,
    @SerialName("editCountByDay") private val mEditCountByDay: JsonElement? = null,
    @SerialName("editCountByTaskType") private val mEditCountByTaskType: JsonElement? = null,
    val totalUserEditCount: Int = 0,
    val totalEditsCount: Int = 0,
    val newcomerTaskEditCount: Int = 0,
    val revertedEditCount: Int = 0,
    val lastEditTimestamp: Long = 0,
    val totalArticlesCreatedCount: Int = 0,
    @SerialName("longestEditingStreak") private val mLongestEditingStreak: JsonElement? = null,
    @SerialName("dailyTotalViews") private val mDailyTotalViews: JsonElement? = null,
    val totalPageviewsCount: Long = 0,
    @SerialName("topViewedArticles") private val mTopViewedArticles: JsonElement? = null,
) {
    // All of these properties need to be lazily initialized from a generic JsonElement because
    // of an annoying quirk in the way PHP serializes JSON objects. If the object is empty, it
    // serializes as an empty array, not an empty object, which causes kotlinx.serialization to
    // fail. Therefore we need to conditionally deserialize these items at runtime.
    val editCountByNamespace: Map<Int, Int> by lazy { if (mEditCountByNamespace is JsonObject) { JsonUtil.json.decodeFromJsonElement(mEditCountByNamespace) } else { emptyMap() } }
    val editCountByDay: Map<String, Int> by lazy { if (mEditCountByDay is JsonObject) { JsonUtil.json.decodeFromJsonElement(mEditCountByDay) } else { emptyMap() } }
    val editCountByTaskType: Map<String, Int> by lazy { if (mEditCountByTaskType is JsonObject) { JsonUtil.json.decodeFromJsonElement(mEditCountByTaskType) } else { emptyMap() } }
    val dailyTotalViews: Map<String, Int> by lazy { if (mDailyTotalViews is JsonObject) { JsonUtil.json.decodeFromJsonElement(mDailyTotalViews) } else { emptyMap() } }
    val topViewedArticles: Map<String, ArticleViews> by lazy { if (mTopViewedArticles is JsonObject) { JsonUtil.json.decodeFromJsonElement(mTopViewedArticles) } else { emptyMap() } }
    val longestEditingStreak: EditStreak? by lazy { if (mLongestEditingStreak is JsonObject) { JsonUtil.json.decodeFromJsonElement(mLongestEditingStreak) } else { null } }

    @Serializable
    class ArticleViews(
        val firstEditDate: String = "",
        val newestEdit: String = "",
        val imageUrl: String = "",
        val viewsCount: Long = 0,
        private val views: JsonElement? = null
    ) {
        val viewsByDay: Map<String, Int> by lazy { if (views is JsonObject) { JsonUtil.json.decodeFromJsonElement(views) } else { emptyMap() } }
    }

    @Serializable
    class EditStreak(
        val datePeriod: EditDateRange? = null,
        val totalEditCountForPeriod: Int = 0
    )

    @Serializable
    class EditDateRange(
        val start: String = "",
        val end: String = "",
        val days: Int = 0,
    )
}
