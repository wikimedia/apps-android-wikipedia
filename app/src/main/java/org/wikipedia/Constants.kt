package org.wikipedia

object Constants {

    // Keep loader IDs unique to each loader. If the loader specified by the ID already exists, the
    // last created loader is reused.
    const val RECENT_SEARCHES_FRAGMENT_LOADER_ID = 101
    const val PLAIN_TEXT_MIME_TYPE = "text/plain"
    const val ACTIVITY_REQUEST_SETTINGS = 41
    const val ACTIVITY_REQUEST_CREATE_ACCOUNT = 42
    const val ACTIVITY_REQUEST_RESET_PASSWORD = 43
    const val ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 44
    const val ACTIVITY_REQUEST_VOICE_SEARCH = 45
    const val ACTIVITY_REQUEST_LANGLINKS = 50
    const val ACTIVITY_REQUEST_EDIT_SECTION = 51
    const val ACTIVITY_REQUEST_GALLERY = 52
    const val ACTIVITY_REQUEST_LOGIN = 53
    const val ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS = 54
    const val ACTIVITY_REQUEST_DESCRIPTION_EDIT = 55
    const val ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL = 56
    const val ACTIVITY_REQUEST_INITIAL_ONBOARDING = 57
    const val ACTIVITY_REQUEST_FEED_CONFIGURE = 58
    const val ACTIVITY_REQUEST_ADD_A_LANGUAGE = 59
    const val ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH = 60
    const val ACTIVITY_REQUEST_BROWSE_TABS = 61
    const val ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY = 62
    const val ACTIVITY_REQUEST_SUGGESTED_EDITS_ONBOARDING = 63
    const val ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT = 64
    const val ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING = 65
    const val ACTIVITY_REQUEST_IMAGE_TAGS_EDIT = 66
    const val ACTIVITY_REQUEST_IMAGE_RECS_ONBOARDING = 67
    const val INTENT_RETURN_TO_MAIN = "returnToMain"
    const val INTENT_FEATURED_ARTICLE_FROM_WIDGET = "featuredArticleFromWidget"
    const val INTENT_APP_SHORTCUT_CONTINUE_READING = "appShortcutContinueReading"
    const val INTENT_APP_SHORTCUT_RANDOMIZER = "appShortcutRandomizer"
    const val INTENT_APP_SHORTCUT_SEARCH = "appShortcutSearch"
    const val INTENT_EXTRA_REVERT_QNUMBER = "revertQNumber"
    const val INTENT_EXTRA_DELETE_READING_LIST = "deleteReadingList"
    const val INTENT_EXTRA_NOTIFICATION_ID = "notificationId"
    const val INTENT_EXTRA_NOTIFICATION_TYPE = "notificationType"
    const val INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME = "syncPauseResume"
    const val INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL = "syncCancel"
    const val INTENT_EXTRA_GO_TO_MAIN_TAB = "goToMainTab"
    const val INTENT_EXTRA_GO_TO_SE_TAB = "goToSETab"
    const val INTENT_EXTRA_INVOKE_SOURCE = "invokeSource"
    const val INTENT_EXTRA_ACTION = "intentAction"
    const val INTENT_EXTRA_HAS_TRANSITION_ANIM = "hasTransitionAnim"
    const val SUGGESTION_REQUEST_ITEMS = 5
    const val API_QUERY_MAX_TITLES = 50
    const val PREFERRED_CARD_THUMBNAIL_SIZE = 800
    const val PREFERRED_GALLERY_IMAGE_SIZE = 1280
    const val MAX_TABS = 100
    const val MAX_READING_LIST_ARTICLE_LIMIT = 5000
    const val MAX_READING_LISTS_LIMIT = 100
    const val MIN_LANGUAGES_TO_UNLOCK_TRANSLATION = 2

    enum class InvokeSource(name: String) {
        CONTEXT_MENU("contextMenu"),
        LINK_PREVIEW_MENU("linkPreviewMenu"),
        PAGE_OVERFLOW_MENU("pageOverflowMenu"),
        NAV_MENU("navMenu"),
        MAIN_ACTIVITY("main"),
        PAGE_ACTIVITY("page"),
        NEWS_ACTIVITY("news"),
        READING_LIST_ACTIVITY("readingList"),
        MOST_READ_ACTIVITY("mostRead"),
        RANDOM_ACTIVITY("random"),
        ON_THIS_DAY_ACTIVITY("onThisDay"),
        GALLERY_ACTIVITY("gallery"),
        READ_MORE_BOOKMARK_BUTTON("readMoreBookmark"),
        BOOKMARK_BUTTON("bookmark"),
        SUGGESTED_EDITS("suggestedEdits"),
        ONBOARDING_DIALOG("onboarding"),
        FEED("feed"),
        NOTIFICATION("notification"),
        APP_SHORTCUTS("appShortcuts"),
        TOOLBAR("toolbar"),
        WIDGET("widget"),
        INTENT_SHARE("intentShare"),
        INTENT_PROCESS_TEXT("intentProcessText"),
        INTENT_UNKNOWN("intentUnknown"),
        FEED_BAR("feedBar"),
        VOICE("voice"),
        ON_THIS_DAY_CARD_BODY("onThisDayCard"),
        ON_THIS_DAY_CARD_FOOTER("onThisDayCardFooter"),
        ON_THIS_DAY_CARD_YEAR("onThisDayCardYear"),
        LEAD_IMAGE("leadImage"),
        TABS_ACTIVITY("tabsActivity"),
        FILE_PAGE_ACTIVITY("filePage"),
        SNACKBAR_ACTION("snackbar"),
        PAGE_ACTION_TAB("pageActionTab"),
        TALK_ACTIVITY("talkActivity"),
        SETTINGS("settings"),
        DIFF_ACTIVITY("diffActivity"),
        WATCHLIST_ACTIVITY("watchlist"),
        SEARCH("search"),
        ANNOUNCEMENT("announcement"),
        LANG_VARIANT_DIALOG("lang_variant_dialog")
    }

    enum class ImageEditType(name: String) {
        ADD_CAPTION("addCaption"),
        ADD_CAPTION_TRANSLATION("addCaptionTranslation"),
        ADD_TAGS("addTags")
    }
}
