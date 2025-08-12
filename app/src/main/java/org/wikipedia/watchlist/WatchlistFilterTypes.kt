package org.wikipedia.watchlist

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode

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
        val LATEST_REVISIONS_GROUP = listOf(LATEST_REVISION, NOT_LATEST_REVISION)
        val USER_STATUS_GROUP = listOf(ALL_USERS, UNREGISTERED, REGISTERED)

        // Multiple choice
        val DEFAULT_FILTER_TYPE_OF_CHANGES = setOf(PAGE_EDITS, PAGE_CREATIONS, LOGGED_ACTIONS)
        // Single choice
        val DEFAULT_FILTER_OTHERS = setOf(ALL_EDITS, ALL_CHANGES, LATEST_REVISION, ALL_EDITORS, ALL_USERS)
        val DEFAULT_FILTER_TYPE_SET = DEFAULT_FILTER_OTHERS + DEFAULT_FILTER_TYPE_OF_CHANGES

        fun find(id: String): WatchlistFilterTypes {
            return entries.find { id == it.id || id.startsWith(it.id) } ?: entries[0]
        }

        fun findGroup(id: String): List<WatchlistFilterTypes> {
            val groups = listOf(MINOR_EDITS_GROUP, BOT_EDITS_GROUP, UNSEEN_CHANGES_GROUP, LATEST_REVISIONS_GROUP, USER_STATUS_GROUP)
            return groups.find { it.contains(find(id)) }.orEmpty()
        }
    }
}
