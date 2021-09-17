package org.wikipedia.analytics

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.json.GsonUtil
import java.lang.reflect.Type

class SuggestedEditsFunnel private constructor(app: WikipediaApp, private val invokeSource: InvokeSource) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    private val parentSessionToken = app.sessionFunnel.sessionToken
    private var helpOpenedCount = 0
    private var contributionsOpenedCount = 0
    private val statsCollection = SuggestedEditStatsCollection()
    private val uniqueTitles = mutableListOf<String>()

    override fun preprocessSessionToken(eventData: JSONObject) {
        preprocessData(eventData, "session_token", parentSessionToken)
    }

    fun impression(action: DescriptionEditActivity.Action) {
        when {
            action === DescriptionEditActivity.Action.ADD_DESCRIPTION -> statsCollection.addDescriptionStats.impressions++
            action === DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> statsCollection.translateDescriptionStats.impressions++
            action === DescriptionEditActivity.Action.ADD_CAPTION -> statsCollection.addCaptionStats.impressions++
            action === DescriptionEditActivity.Action.TRANSLATE_CAPTION -> statsCollection.translateCaptionStats.impressions++
            action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS -> statsCollection.imageTagStats.impressions++
        }
    }

    fun click(title: String, action: DescriptionEditActivity.Action) {
        val stats: SuggestedEditStats = when {
            action === DescriptionEditActivity.Action.ADD_DESCRIPTION -> statsCollection.addDescriptionStats
            action === DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> statsCollection.translateDescriptionStats
            action === DescriptionEditActivity.Action.ADD_CAPTION -> statsCollection.addCaptionStats
            action === DescriptionEditActivity.Action.TRANSLATE_CAPTION -> statsCollection.translateCaptionStats
            else -> return
        }
        stats.clicks++
        if (!uniqueTitles.contains(title)) {
            uniqueTitles.add(title)
            val maxItems = 100
            if (uniqueTitles.size > maxItems) {
                uniqueTitles.removeAt(0)
            }
            stats.suggestionsClicked++
        }
    }

    fun cancel(action: DescriptionEditActivity.Action) {
        when {
            action === DescriptionEditActivity.Action.ADD_DESCRIPTION -> statsCollection.addDescriptionStats.cancels++
            action === DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> statsCollection.translateDescriptionStats.cancels++
            action === DescriptionEditActivity.Action.ADD_CAPTION -> statsCollection.addCaptionStats.cancels++
            action === DescriptionEditActivity.Action.TRANSLATE_CAPTION -> statsCollection.translateCaptionStats.cancels++
        }
    }

    fun success(action: DescriptionEditActivity.Action) {
        when {
            action === DescriptionEditActivity.Action.ADD_DESCRIPTION -> statsCollection.addDescriptionStats.successes++
            action === DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> statsCollection.translateDescriptionStats.successes++
            action === DescriptionEditActivity.Action.ADD_CAPTION -> statsCollection.addCaptionStats.successes++
            action === DescriptionEditActivity.Action.TRANSLATE_CAPTION -> statsCollection.translateCaptionStats.successes++
            action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS -> statsCollection.imageTagStats.successes++
        }
    }

    fun failure(action: DescriptionEditActivity.Action) {
        when {
            action === DescriptionEditActivity.Action.ADD_DESCRIPTION -> statsCollection.addDescriptionStats.failures++
            action === DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> statsCollection.translateDescriptionStats.failures++
            action === DescriptionEditActivity.Action.ADD_CAPTION -> statsCollection.addCaptionStats.failures++
            action === DescriptionEditActivity.Action.TRANSLATE_CAPTION -> statsCollection.translateCaptionStats.failures++
            action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS -> statsCollection.imageTagStats.failures++
        }
    }

    fun helpOpened() {
        helpOpenedCount++
    }

    fun contributionsOpened() {
        contributionsOpenedCount++
    }

    fun log() {
        log(
                "edit_tasks", GsonUtil.getDefaultGson().newBuilder()
                .registerTypeAdapter(SuggestedEditStats::class.java, SuggestedEditsStatsTypeAdapter())
                .create().toJson(statsCollection),
                "help_opened", helpOpenedCount,
                "scorecard_opened", contributionsOpenedCount,
                "source", invokeSource.value
        )
    }

    private class SuggestedEditsStatsTypeAdapter : JsonSerializer<SuggestedEditStats> {
        override fun serialize(src: SuggestedEditStats, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return if (src.isEmpty) JsonNull.INSTANCE else GsonUtil.getDefaultGson().toJsonTree(src, typeOfSrc)
        }
    }

    private class SuggestedEditStatsCollection {
        @SerializedName("a-d")
        val addDescriptionStats = SuggestedEditStats()

        @SerializedName("t-d")
        val translateDescriptionStats = SuggestedEditStats()

        @SerializedName("a-c")
        val addCaptionStats = SuggestedEditStats()

        @SerializedName("t-c")
        val translateCaptionStats = SuggestedEditStats()

        @SerializedName("i-t")
        val imageTagStats = SuggestedEditStats()
    }

    private class SuggestedEditStats {
        @SerializedName("imp")
        var impressions = 0

        @SerializedName("clk")
        var clicks = 0

        @SerializedName("sg")
        var suggestionsClicked = 0

        @SerializedName("cxl")
        var cancels = 0

        @SerializedName("suc")
        var successes = 0

        @SerializedName("fl")
        var failures = 0
        val isEmpty = impressions == 0 && clicks == 0 && suggestionsClicked == 0 && cancels == 0 &&
                successes == 0 && failures == 0
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppSuggestedEdits"
        private const val REV_ID = 18949003
        private const val SUGGESTED_EDITS_UI_VERSION = "1.0"
        private const val SUGGESTED_EDITS_API_VERSION = "1.0"
        private var INSTANCE: SuggestedEditsFunnel? = null
        const val SUGGESTED_EDITS_ADD_COMMENT = "#suggestededit-add $SUGGESTED_EDITS_UI_VERSION"
        const val SUGGESTED_EDITS_TRANSLATE_COMMENT = "#suggestededit-translate $SUGGESTED_EDITS_UI_VERSION"
        const val SUGGESTED_EDITS_IMAGE_TAG_AUTO_COMMENT = "#suggestededit-imgtag-auto $SUGGESTED_EDITS_UI_VERSION"
        const val SUGGESTED_EDITS_IMAGE_TAG_CUSTOM_COMMENT = "#suggestededit-imgtag-custom $SUGGESTED_EDITS_UI_VERSION"

        operator fun get(invokeSource: InvokeSource): SuggestedEditsFunnel {
            if (INSTANCE == null) {
                INSTANCE = SuggestedEditsFunnel(WikipediaApp.getInstance(), invokeSource)
            } else if (INSTANCE!!.invokeSource != invokeSource) {
                INSTANCE?.log()
                INSTANCE = SuggestedEditsFunnel(WikipediaApp.getInstance(), invokeSource)
            }
            return INSTANCE!!
        }

        fun get(): SuggestedEditsFunnel {
            return if (INSTANCE != null && INSTANCE!!.invokeSource != InvokeSource.SUGGESTED_EDITS) {
                INSTANCE!!
            } else Companion[InvokeSource.SUGGESTED_EDITS]
        }

        fun reset() {
            INSTANCE = null
        }
    }
}
