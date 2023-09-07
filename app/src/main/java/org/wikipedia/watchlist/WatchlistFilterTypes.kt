package org.wikipedia.watchlist

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

@Suppress("unused")
enum class WatchlistFilterTypes constructor(val id: String,
                                            @StringRes val title: Int,
                                            val value: String
) : EnumCode {
    PAGE_EDITS("pageEdits",
        R.string.watchlist_filter_type_of_change_page_edits, "edit"),
    PAGE_CREATIONS("pageCreations",
        R.string.watchlist_filter_type_of_change_page_creations, "new"),
    CATEGORY_CHANGES("categoryChanges",
        R.string.watchlist_filter_type_of_change_category_changes, "categorize"),
    WIKIDATA_EDITS("wikidataEdits",
        R.string.watchlist_filter_type_of_change_wikidata_edits, "external"),
    LOGGED_ACTIONS("loggedActions",
        R.string.watchlist_filter_type_of_change_logged_actions, "log"),
    ALL_EDITS("allEdits",
        R.string.watchlist_filter_all_text, ""),
    MINOR_EDITS("minorEdits",
        R.string.watchlist_filter_significance_minor, "minor"),
    NON_MINOR_EDITS("nonMinorEdits",
        R.string.watchlist_filter_significance_edits, "!minor"),
    ALL_EDITORS("allEditors",
        R.string.watchlist_filter_all_text, ""),
    BOT("bot",
        R.string.watchlist_filter_automated_contributions_bot, "bot"),
    HUMAN("human",
        R.string.watchlist_filter_automated_contributions_human, "!bot"),
    ALL_CHANGES("allChanges",
        R.string.watchlist_filter_all_text, ""),
    UNSEEN_CHANGES("unseenChanges",
        R.string.watchlist_filter_watchlist_activity_unseen, "unread"),
    SEEN_CHANGES("seenChanges",
        R.string.watchlist_filter_watchlist_activity_seen, "!unread"),
    ALL_REVISIONS("allRevisions",
        R.string.watchlist_filter_all_text, ""),
    LATEST_REVISION("latestRevision",
        R.string.watchlist_filter_latest_revisions_latest_revision, ""),
    NOT_LATEST_REVISION("notLatestRevision",
        R.string.watchlist_filter_latest_revisions_not_latest_revision, "1"),
    ALL_USERS("allUsers",
        R.string.watchlist_filter_all_text, ""),
    UNREGISTERED("unregistered",
        R.string.watchlist_filter_user_status_unregistered, "anon"),
    REGISTERED("registered",
        R.string.watchlist_filter_user_status_registered, "!anon");
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
//    QUALITY_GOOD("qualityGood",
//        R.string.watchlist_filter_contribution_quality_good),
//    QUALITY_MAY_PROBLEMS("qualityMayProblems",
//        R.string.watchlist_filter_contribution_quality_may_problems),
//    QUALITY_LIKELY_PROBLEMS("qualityLikelyProblems",
//        R.string.watchlist_filter_contribution_quality_likely_problems),
//    QUALITY_VERY_LIKELY_PROBLEMS("qualityBad",
//        R.string.watchlist_filter_contribution_quality_bad),
//    INTENT_GOOD("intentGood",
//        R.string.watchlist_filter_user_intent_good),
//    INTENT_MAY_PROBLEMS("intentMayProblems",
//        R.string.watchlist_filter_user_intent_may_problems),
//    INTENT_LIKELY_PROBLEMS("intentLikelyProblems",
//        R.string.watchlist_filter_user_intent_likely_problems),
//    INTENT_VERY_LIKELY_PROBLEMS("intentBad",
//        R.string.watchlist_filter_user_intent_bad),;

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {
        val TYPE_OF_CHANGES_GROUP = listOf(PAGE_EDITS, PAGE_CREATIONS, CATEGORY_CHANGES, WIKIDATA_EDITS, LOGGED_ACTIONS)
        val MINOR_EDITS_GROUP = listOf(ALL_EDITS, MINOR_EDITS, NON_MINOR_EDITS)
        val BOT_EDITS_GROUP = listOf(ALL_EDITORS, BOT, HUMAN)
        val UNSEEN_CHANGES_GROUP = listOf(ALL_CHANGES, UNSEEN_CHANGES, SEEN_CHANGES)
        val LATEST_REVISIONS_GROUP = listOf(ALL_REVISIONS, LATEST_REVISION, NOT_LATEST_REVISION)
        val USER_STATUS_GROUP = listOf(ALL_USERS, UNREGISTERED, REGISTERED)

        // Multiple choice
        val DEFAULT_FILTER_TYPE_OF_CHANGES = setOf(PAGE_EDITS, PAGE_CREATIONS, LOGGED_ACTIONS)
        // Single choice
        val DEFAULT_FILTER_OTHERS = setOf(ALL_EDITS, ALL_CHANGES, ALL_REVISIONS, ALL_EDITORS, ALL_USERS)
        val DEFAULT_FILTER_TYPE_SET = DEFAULT_FILTER_OTHERS + DEFAULT_FILTER_TYPE_OF_CHANGES

        private val MAP = EnumCodeMap(WatchlistFilterTypes::class.java)

        private fun findOrNull(id: String): WatchlistFilterTypes? {
            return MAP.valueIterator().asSequence().firstOrNull { id == it.id || id.startsWith(it.id) }
        }

        fun find(id: String): WatchlistFilterTypes {
            return findOrNull(id) ?: MAP[0]
        }

        fun findGroup(id: String): List<WatchlistFilterTypes> {
            val groups = listOf(MINOR_EDITS_GROUP, BOT_EDITS_GROUP, UNSEEN_CHANGES_GROUP, LATEST_REVISIONS_GROUP, USER_STATUS_GROUP)
            return groups.find { it.contains(find(id)) }.orEmpty()
        }
    }
}
