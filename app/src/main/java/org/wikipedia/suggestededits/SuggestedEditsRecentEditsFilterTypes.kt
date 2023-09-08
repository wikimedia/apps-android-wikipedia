package org.wikipedia.suggestededits

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

@Suppress("unused")
enum class SuggestedEditsRecentEditsFilterTypes constructor(val id: String,
                                                            @StringRes val title: Int,
                                                            val value: String
) : EnumCode {
    ALL_EDITS("allEdits",
        R.string.watchlist_filter_all_text, ""),
    MINOR_EDITS("minorEdits",
        R.string.patroller_tasks_filters_significance_minor, "minor"),
    NON_MINOR_EDITS("nonMinorEdits",
        R.string.patroller_tasks_filters_significance_edits, "!minor"),
    ALL_EDITORS("allEditors",
        R.string.watchlist_filter_all_text, ""),
    BOT("bot",
        R.string.patroller_tasks_filters_automated_contributions_bot, "bot"),
    HUMAN("human",
        R.string.patroller_tasks_filters_automated_contributions_human, "!bot"),
    ALL_CHANGES("allChanges",
        R.string.watchlist_filter_all_text, ""),
    ALL_REVISIONS("allRevisions",
        R.string.watchlist_filter_all_text, ""),
    LATEST_REVISION("latestRevision",
        R.string.patroller_tasks_filters_latest_revisions_latest_revision, ""),
    NOT_LATEST_REVISION("notLatestRevision",
        R.string.patroller_tasks_filters_latest_revisions_not_latest_revision, "1"),
    ALL_USERS("allUsers",
        R.string.watchlist_filter_all_text, ""),
    UNREGISTERED("unregistered",
        R.string.patroller_tasks_filters_user_status_unregistered, "anon"),
    REGISTERED("registered",
        R.string.patroller_tasks_filters_user_status_registered, "!anon"),
    // ORES score reference: https://github.com/wikimedia/mediawiki-extensions-ORES/blob/master/extension.json#L201-L232C28
    QUALITY_GOOD("qualityGood",
        R.string.patroller_tasks_filters_contribution_quality_good, "0|0.699"),
    QUALITY_MAY_PROBLEMS("qualityMayProblems",
        R.string.patroller_tasks_filters_contribution_quality_may_problems, "0.149|1"),
    QUALITY_LIKELY_PROBLEMS("qualityLikelyProblems",
        R.string.patroller_tasks_filters_contribution_quality_likely_problems, "0.629|1"),
    QUALITY_VERY_LIKELY_PROBLEMS("qualityBad",
        R.string.patroller_tasks_filters_contribution_quality_bad, "0.944|1"),
    INTENT_GOOD("intentGood",
        R.string.patroller_tasks_filters_user_intent_good, "0.777|1"),
    INTENT_MAY_PROBLEMS("intentMayProblems",
        R.string.patroller_tasks_filters_user_intent_may_problems, "0|0.075"),
    INTENT_LIKELY_PROBLEMS("intentLikelyProblems",
        R.string.patroller_tasks_filters_user_intent_likely_problems, "0|0.647"),
    INTENT_VERY_LIKELY_PROBLEMS("intentBad",
        R.string.patroller_tasks_filters_user_intent_bad, "false");
    // TODO: implement for the next iteration
//    NEW_USERS("newUsers",
//        R.string.watchlist_filter_type_of_change_new_users,
//        "wltype", "log"),
//    NEWCOMERS("newcomers",
//        R.string.watchlist_filter_user_status_newcomers),
//    LEARNERS("learners",
//        R.string.watchlist_filter_user_status_learners),
//    EXPERIENCED_USERS("experiencedUsers",
//        R.string.watchlist_filter_user_status_experienced),
//    CHANGES_BY_YOU("changesByYou",
//        R.string.watchlist_filter_contribution_authorship_own),
//    CHANGES_BY_OTHERS("changesByOthers",
//        R.string.watchlist_filter_contribution_authorship_except_own),

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {
        val MINOR_EDITS_GROUP = listOf(ALL_EDITS, MINOR_EDITS, NON_MINOR_EDITS)
        val BOT_EDITS_GROUP = listOf(ALL_EDITORS, BOT, HUMAN)
        val LATEST_REVISIONS_GROUP = listOf(ALL_REVISIONS, LATEST_REVISION, NOT_LATEST_REVISION)
        val USER_STATUS_GROUP = listOf(ALL_USERS, UNREGISTERED, REGISTERED)
        val CONTRIBUTION_QUALITY_GROUP = listOf(QUALITY_GOOD, QUALITY_MAY_PROBLEMS, QUALITY_LIKELY_PROBLEMS, QUALITY_VERY_LIKELY_PROBLEMS)
        val USER_INTENT_GROUP = listOf(INTENT_GOOD, INTENT_MAY_PROBLEMS, INTENT_LIKELY_PROBLEMS, INTENT_VERY_LIKELY_PROBLEMS)
        val DEFAULT_FILTER_TYPE_SET = setOf(ALL_EDITS, ALL_CHANGES, ALL_REVISIONS, ALL_EDITORS, ALL_USERS)

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
