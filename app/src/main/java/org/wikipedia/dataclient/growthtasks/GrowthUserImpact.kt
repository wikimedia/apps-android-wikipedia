package org.wikipedia.dataclient.growthtasks

import android.text.format.DateUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.minusMonth
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.yearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.json.InstantUnixSecondsSerializer
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import java.time.Instant
import java.time.LocalDate as JavaLocalDate

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
    @Serializable(InstantUnixSecondsSerializer::class) val lastEditTimestamp: Instant = Instant.EPOCH,
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
    val editCountByDay: Map<LocalDate, Int> by lazy { if (mEditCountByDay is JsonObject) { JsonUtil.json.decodeFromJsonElement(mEditCountByDay) } else { emptyMap() } }
    val editCountByTaskType: Map<String, Int> by lazy { if (mEditCountByTaskType is JsonObject) { JsonUtil.json.decodeFromJsonElement(mEditCountByTaskType) } else { emptyMap() } }
    val dailyTotalViews: Map<LocalDate, Int> by lazy { if (mDailyTotalViews is JsonObject) { JsonUtil.json.decodeFromJsonElement(mDailyTotalViews) } else { emptyMap() } }
    val topViewedArticles: Map<String, ArticleViews> by lazy { if (mTopViewedArticles is JsonObject) { JsonUtil.json.decodeFromJsonElement(mTopViewedArticles) } else { emptyMap() } }
    val longestEditingStreak: EditStreak? by lazy { if (mLongestEditingStreak is JsonObject) { JsonUtil.json.decodeFromJsonElement(mLongestEditingStreak) } else { null } }

    val groupEditsByMonth: Map<YearMonth, Int> by lazy {
        editCountByDay.entries.groupBy { it.key.yearMonth }
            .mapValues { it.value.sumOf { entry -> entry.value } }
    }

    var topViewedArticlesWithPageTitle: Map<PageTitle, ArticleViews> = emptyMap()

    private val today by lazy { JavaLocalDate.now().toKotlinLocalDate() }
    val editsThisMonth by lazy { groupEditsByMonth[today.yearMonth] ?: 0 }
    val editsLastMonth by lazy { groupEditsByMonth[today.yearMonth.minusMonth()] ?: 0 }

    val lastThirtyDaysEdits by lazy {
        val thirtyDaysAgo = today - DatePeriod(days = 30)
        // Some days do not have a key, fill them with 0
        (thirtyDaysAgo..today).associateWith { date -> (editCountByDay[date] ?: 0) }
    }

    val lastEditRelativeTime by lazy {
        val time = lastEditTimestamp.toEpochMilli()
        DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), 0L).toString()
    }

    @Serializable
    class ArticleViews(
        val firstEditDate: LocalDate,
        val newestEdit: String = "",
        val imageUrl: String = "",
        val viewsCount: Long = 0,
        private val views: JsonElement? = null
    ) {
        val viewsByDay: Map<LocalDate, Int> by lazy { if (views is JsonObject) { JsonUtil.json.decodeFromJsonElement(views) } else { emptyMap() } }
    }

    @Serializable
    class EditStreak(
        val datePeriod: EditDateRange? = null,
        val totalEditCountForPeriod: Int = 0
    )

    @Serializable
    class EditDateRange(
        val start: LocalDate,
        val end: LocalDate,
        val days: Int = 0,
    )
}
