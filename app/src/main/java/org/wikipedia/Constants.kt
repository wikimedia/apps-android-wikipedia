package org.wikipedia

import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite

object Constants {

    const val ACTIVITY_REQUEST_ADD_A_LANGUAGE = 59
    const val ACTIVITY_REQUEST_BROWSE_TABS = 61
    const val ACTIVITY_REQUEST_DESCRIPTION_EDIT = 55
    const val ACTIVITY_REQUEST_FEED_CONFIGURE = 58
    const val ACTIVITY_REQUEST_GALLERY = 52
    const val ACTIVITY_REQUEST_LOGIN = 53
    const val ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY = 62
    const val ACTIVITY_REQUEST_SETTINGS = 41
    const val ACTIVITY_REQUEST_VOICE_SEARCH = 45

    const val ARG_TITLE = "title"
    const val ARG_WIKISITE = "wikiSite"
    const val ARG_TEXT = "text"
    const val ARG_BOOLEAN = "boolean"
    const val INTENT_APP_SHORTCUT_CONTINUE_READING = "appShortcutContinueReading"
    const val INTENT_APP_SHORTCUT_RANDOMIZER = "appShortcutRandomizer"
    const val INTENT_APP_SHORTCUT_SEARCH = "appShortcutSearch"
    const val INTENT_EXTRA_ACTION = "intentAction"
    const val INTENT_EXTRA_DELETE_READING_LIST = "deleteReadingList"
    const val INTENT_EXTRA_GO_TO_MAIN_TAB = "goToMainTab"
    const val INTENT_EXTRA_GO_TO_SE_TAB = "goToSETab"
    const val INTENT_EXTRA_HAS_TRANSITION_ANIM = "hasTransitionAnim"
    const val INTENT_EXTRA_INVOKE_SOURCE = "invokeSource"
    const val INTENT_EXTRA_PREVIEW_SAVED_READING_LISTS = "previewSavedReadingList"
    const val INTENT_EXTRA_NOTIFICATION_ID = "notificationId"
    const val INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL = "syncCancel"
    const val INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME = "syncPauseResume"
    const val INTENT_EXTRA_NOTIFICATION_TYPE = "notificationType"
    const val INTENT_EXTRA_REVERT_QNUMBER = "revertQNumber"
    const val INTENT_RETURN_TO_MAIN = "returnToMain"

    const val MAX_READING_LIST_ARTICLE_LIMIT = 5000
    const val MAX_READING_LISTS_LIMIT = 100
    const val MAX_TABS = 100
    const val MIN_LANGUAGES_TO_UNLOCK_TRANSLATION = 2
    const val PLAIN_TEXT_MIME_TYPE = "text/plain"
    const val PREFERRED_CARD_THUMBNAIL_SIZE = 800
    const val PREFERRED_GALLERY_IMAGE_SIZE = 1280
    const val SUGGESTION_REQUEST_ITEMS = 5

    const val WIKI_CODE_COMMONS = "commons"
    const val COMMONS_DB_NAME = "commonswiki"
    const val WIKI_CODE_WIKIDATA = "wikidata"
    const val WIKIDATA_DB_NAME = "wikidatawiki"

    val NON_LANGUAGE_SUBDOMAINS = listOf("donate", "thankyou", "quote", "textbook", "sources", "species", "commons", "meta")

    val commonsWikiSite = WikiSite(Service.COMMONS_URL)
    val wikidataWikiSite = WikiSite(Service.WIKIDATA_URL)

    enum class InvokeSource(val value: String) {
        ANNOUNCEMENT("announcement"),
        APP_SHORTCUTS("appShortcuts"),
        ARCHIVED_TALK_ACTIVITY("archivedTalkActivity"),
        BOOKMARK_BUTTON("bookmark"),
        CONTEXT_MENU("contextMenu"),
        DIFF_ACTIVITY("diffActivity"),
        FEED("feed"),
        FEED_BAR("feedBar"),
        FILE_PAGE_ACTIVITY("filePage"),
        GALLERY_ACTIVITY("gallery"),
        INTENT_PROCESS_TEXT("intentProcessText"),
        INTENT_SHARE("intentShare"),
        INTENT_UNKNOWN("intentUnknown"),
        LEAD_IMAGE("leadImage"),
        LINK_PREVIEW_MENU("linkPreviewMenu"),
        MOST_READ_ACTIVITY("mostRead"),
        NAV_MENU("navMenu"),
        NEWS_ACTIVITY("news"),
        NOTIFICATION("notification"),
        ON_THIS_DAY_ACTIVITY("onThisDay"),
        ON_THIS_DAY_CARD_BODY("onThisDayCard"),
        ON_THIS_DAY_CARD_FOOTER("onThisDayCardFooter"),
        ON_THIS_DAY_CARD_YEAR("onThisDayCardYear"),
        ONBOARDING_DIALOG("onboarding"),
        PAGE_ACTION_TAB("pageActionTab"),
        PAGE_ACTIVITY("page"),
        PAGE_DESCRIPTION_CTA("pageDescCta"),
        PAGE_EDIT_PENCIL("pageEditPencil"),
        PAGE_EDIT_HIGHLIGHT("pageEditHighlight"),
        PAGE_OVERFLOW_MENU("pageOverflowMenu"),
        PLACES("places"),
        RANDOM_ACTIVITY("random"),
        READING_LIST_ACTIVITY("readingList"),
        SEARCH("search"),
        SETTINGS("settings"),
        SNACKBAR_ACTION("snackbar"),
        SUGGESTED_EDITS("suggestedEdits"),
        TABS_ACTIVITY("tabsActivity"),
        TALK_TOPICS_ACTIVITY("talkTopicsActivity"),
        TALK_TOPIC_ACTIVITY("talkTopicActivity"),
        TALK_REPLY_ACTIVITY("talkReplyActivity"),
        EDIT_ACTIVITY("editActivity"),
        TOOLBAR("toolbar"),
        VOICE("voice"),
        WATCHLIST_ACTIVITY("watchlist"),
        WATCHLIST_FILTER_ACTIVITY("watchlistFilter"),
        WIDGET("widget"),
        USER_CONTRIB_ACTIVITY("userContribActivity"),
        EDIT_ADD_IMAGE("editAddImage"),
        SUGGESTED_EDITS_RECENT_EDITS("suggestedEditsRecentEdits"),
        RECOMMENDED_CONTENT("recommendedContent"),
        ON_THIS_DAY_GAME_ACTIVITY("onThisDayGame")
    }

    enum class ImageEditType(name: String) {
        ADD_CAPTION("addCaption"),
        ADD_CAPTION_TRANSLATION("addCaptionTranslation"),
        ADD_TAGS("addTags")
    }
}
