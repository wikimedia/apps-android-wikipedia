package org.wikipedia.watchlist

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode

@Suppress("unused")
enum class WatchlistFilterTypes constructor(val id: String,
                                            @StringRes val title: Int,
                                            val attribute: String,
                                            val value: String
) : EnumCode {
    LATEST_REVISION("latestRevision",
        R.string.watchlist_filter_latest_revisions_latest_revision,
        "wlallrev", "true"),
    NOT_LATEST_REVISION("notLatestRevision",
        R.string.watchlist_filter_latest_revisions_not_latest_revision,
        "wlallrev", "false"),
    PAGE_EDITS("pageEdits",
        R.string.watchlist_filter_type_of_change_page_edits,
        "wltype", "edit"),
    PAGE_CREATIONS("pageCreations",
        R.string.watchlist_filter_type_of_change_page_creations,
        "wltype", "new"),
    CATEGORY_CHANGES("categoryChanges",
        R.string.watchlist_filter_type_of_change_category_changes,
        "wltype", "categorize"),
    WIKIDATA_EDITS("wikidataEdits",
        R.string.watchlist_filter_type_of_change_wikidata_edits,
        "wltype", "external"),
    LOGGED_ACTIONS("loggedActions",
        R.string.watchlist_filter_type_of_change_logged_actions,
        "wltype", "log"),
    MINOR_EDITS("minorEdits",
        R.string.watchlist_filter_significance_minor,
        "wlshow", "minor"),
    NON_MINOR_EDITS("nonMinorEdits",
        R.string.watchlist_filter_significance_edits,
        "wlshow", "!minor"),
    BOT("bot",
        R.string.watchlist_filter_automated_contributions_bot,
        "wlshow", "bot"),
    HUMAN("human",
        R.string.watchlist_filter_automated_contributions_human,
        "wlshow", "!bot"),
    UNSEEN_CHANGES("unseenChanges",
        R.string.watchlist_filter_watchlist_activity_unseen,
        "wlshow", "unread"),
    SEEN_CHANGES("seenChanges",
        R.string.watchlist_filter_watchlist_activity_seen,
        "wlshow", "!unread");
    // TODO: implement for the next iteration
//    NEW_USERS("newUsers",
//        R.string.watchlist_filter_type_of_change_new_users,
//        "wltype", "log"),
//    UNREGISTERED("unregistered",
//        R.string.watchlist_filter_user_status_unregistered),
//    REGISTERED("registered",
//        R.string.watchlist_filter_user_status_registered),
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
}