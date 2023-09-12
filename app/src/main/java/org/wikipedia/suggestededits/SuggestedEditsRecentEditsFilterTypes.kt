package org.wikipedia.suggestededits

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

@Suppress("unused")
enum class SuggestedEditsRecentEditsFilterTypes constructor(val id: String,
                                                            val value: String,
                                                            @StringRes val title: Int,
                                                            @StringRes val description: Int? = null
) : EnumCode {
    ALL_EDITS("allEdits", "",
        R.string.patroller_tasks_filters_all_text),
    MINOR_EDITS("minorEdits", "minor",
        R.string.patroller_tasks_filters_significance_minor),
    NON_MINOR_EDITS("nonMinorEdits", "!minor",
        R.string.patroller_tasks_filters_significance_edits),
    ALL_EDITORS("allEditors", "",
        R.string.patroller_tasks_filters_all_text),
    BOT("bot", "bot",
        R.string.patroller_tasks_filters_automated_contributions_bot),
    HUMAN("human", "!bot",
        R.string.patroller_tasks_filters_automated_contributions_human),
    LATEST_REVISION("latestRevision", "",
        R.string.patroller_tasks_filters_latest_revisions_latest_revision),
    NOT_LATEST_REVISION("notLatestRevision", "1",
        R.string.patroller_tasks_filters_latest_revisions_not_latest_revision),
    ALL_USERS("allUsers", "",
        R.string.patroller_tasks_filters_all_text),
    UNREGISTERED("unregistered", "anon",
        R.string.patroller_tasks_filters_user_status_unregistered, R.string.patroller_tasks_filters_user_status_unregistered_desc),
    REGISTERED("registered", "!anon",
        R.string.patroller_tasks_filters_user_status_registered, R.string.patroller_tasks_filters_user_status_registered_desc),
    // ORES score reference: https://github.com/wikimedia/mediawiki-extensions-ORES/blob/master/extension.json#L201-L232C28
    QUALITY_GOOD("qualityGood", "0|0.699",
        R.string.patroller_tasks_filters_contribution_quality_good, R.string.patroller_tasks_filters_contribution_quality_good_desc),
    QUALITY_MAY_PROBLEMS("qualityMayProblems", "0.149|1",
        R.string.patroller_tasks_filters_contribution_quality_may_problems, R.string.patroller_tasks_filters_contribution_quality_may_problems_desc),
    QUALITY_LIKELY_PROBLEMS("qualityLikelyProblems", "0.629|1",
        R.string.patroller_tasks_filters_contribution_quality_likely_problems, R.string.patroller_tasks_filters_contribution_quality_likely_problems_desc),
    QUALITY_VERY_LIKELY_PROBLEMS("qualityBad", "0.944|1",
        R.string.patroller_tasks_filters_contribution_quality_bad, R.string.patroller_tasks_filters_contribution_quality_bad_desc),
    INTENT_GOOD("intentGood", "0.777|1",
        R.string.patroller_tasks_filters_user_intent_good, R.string.patroller_tasks_filters_user_intent_good_desc),
    INTENT_MAY_PROBLEMS("intentMayProblems", "0|0.075",
        R.string.patroller_tasks_filters_user_intent_may_problems, R.string.patroller_tasks_filters_user_intent_may_problems_desc),
    INTENT_LIKELY_PROBLEMS("intentLikelyProblems", "0|0.647",
        R.string.patroller_tasks_filters_user_intent_likely_problems, R.string.patroller_tasks_filters_user_intent_likely_problems_desc),
    INTENT_VERY_LIKELY_PROBLEMS("intentBad", "false",
        R.string.patroller_tasks_filters_user_intent_bad, R.string.patroller_tasks_filters_user_intent_bad_desc);

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {
        val MINOR_EDITS_GROUP = listOf(ALL_EDITS, MINOR_EDITS, NON_MINOR_EDITS)
        val BOT_EDITS_GROUP = listOf(ALL_EDITORS, BOT, HUMAN)
        val LATEST_REVISIONS_GROUP = listOf(LATEST_REVISION, NOT_LATEST_REVISION)
        val USER_STATUS_GROUP = listOf(ALL_USERS, UNREGISTERED, REGISTERED)
        val CONTRIBUTION_QUALITY_GROUP = listOf(QUALITY_GOOD, QUALITY_MAY_PROBLEMS, QUALITY_LIKELY_PROBLEMS, QUALITY_VERY_LIKELY_PROBLEMS)
        val USER_INTENT_GROUP = listOf(INTENT_GOOD, INTENT_MAY_PROBLEMS, INTENT_LIKELY_PROBLEMS, INTENT_VERY_LIKELY_PROBLEMS)
        val DEFAULT_FILTER_TYPE_SET = setOf(ALL_EDITS, LATEST_REVISION, ALL_EDITORS, ALL_USERS)

        private val MAP = EnumCodeMap(SuggestedEditsRecentEditsFilterTypes::class.java)

        private fun findOrNull(id: String): SuggestedEditsRecentEditsFilterTypes? {
            return MAP.valueIterator().asSequence().firstOrNull { id == it.id || id.startsWith(it.id) }
        }

        fun find(id: String): SuggestedEditsRecentEditsFilterTypes {
            return findOrNull(id) ?: MAP[0]
        }

        fun findGroup(id: String): List<SuggestedEditsRecentEditsFilterTypes> {
            val groups = listOf(MINOR_EDITS_GROUP, BOT_EDITS_GROUP, LATEST_REVISIONS_GROUP, USER_STATUS_GROUP)
            return groups.find { it.contains(find(id)) }.orEmpty()
        }
    }
}
