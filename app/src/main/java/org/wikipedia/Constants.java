package org.wikipedia;

public final class Constants {
    public static final String PLAIN_TEXT_MIME_TYPE = "text/plain";

    public static final int ACTIVITY_REQUEST_SETTINGS = 41;
    public static final int ACTIVITY_REQUEST_CREATE_ACCOUNT = 42;
    public static final int ACTIVITY_REQUEST_RESET_PASSWORD = 43;
    public static final int ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 44;
    public static final int ACTIVITY_REQUEST_VOICE_SEARCH = 45;
    public static final int ACTIVITY_REQUEST_LANGLINKS = 50;
    public static final int ACTIVITY_REQUEST_EDIT_SECTION = 51;
    public static final int ACTIVITY_REQUEST_GALLERY = 52;
    public static final int ACTIVITY_REQUEST_LOGIN = 53;
    public static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS = 54;
    public static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT = 55;
    public static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL = 56;
    public static final int ACTIVITY_REQUEST_INITIAL_ONBOARDING = 57;
    public static final int ACTIVITY_REQUEST_FEED_CONFIGURE = 58;
    public static final int ACTIVITY_REQUEST_ADD_A_LANGUAGE = 59;
    public static final int ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH = 60;
    public static final int ACTIVITY_REQUEST_BROWSE_TABS = 61;
    public static final int ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY = 62;
    public static final int ACTIVITY_REQUEST_SUGGESTED_EDITS_ONBOARDING = 63;
    public static final int ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT = 64;
    public static final int ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING = 65;
    public static final int ACTIVITY_REQUEST_IMAGE_TAGS_EDIT = 66;
    public static final int ACTIVITY_REQUEST_IMAGE_RECS_ONBOARDING = 67;

    public static final String INTENT_RETURN_TO_MAIN = "returnToMain";
    public static final String INTENT_FEATURED_ARTICLE_FROM_WIDGET = "featuredArticleFromWidget";

    public static final String INTENT_APP_SHORTCUT_CONTINUE_READING = "appShortcutContinueReading";
    public static final String INTENT_APP_SHORTCUT_RANDOMIZER = "appShortcutRandomizer";
    public static final String INTENT_APP_SHORTCUT_SEARCH = "appShortcutSearch";

    public static final String INTENT_EXTRA_REVERT_QNUMBER = "revertQNumber";
    public static final String INTENT_EXTRA_DELETE_READING_LIST = "deleteReadingList";
    public static final String INTENT_EXTRA_NOTIFICATION_ID = "notificationId";
    public static final String INTENT_EXTRA_NOTIFICATION_TYPE = "notificationType";

    public static final String INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME = "syncPauseResume";
    public static final String INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL = "syncCancel";
    public static final String INTENT_EXTRA_GO_TO_MAIN_TAB = "goToMainTab";
    public static final String INTENT_EXTRA_GO_TO_SE_TAB = "goToSETab";
    public static final String INTENT_EXTRA_INVOKE_SOURCE = "invokeSource";
    public static final String INTENT_EXTRA_ACTION = "intentAction";
    public static final String INTENT_EXTRA_HAS_TRANSITION_ANIM = "hasTransitionAnim";


    public static final int SUGGESTION_REQUEST_ITEMS = 5;
    public static final int API_QUERY_MAX_TITLES = 50;

    public static final int PREFERRED_CARD_THUMBNAIL_SIZE = 800;
    public static final int PREFERRED_GALLERY_IMAGE_SIZE = 1280;

    public static final int MAX_TABS = 100;
    public static final int MAX_READING_LIST_ARTICLE_LIMIT = 5000;
    public static final int MAX_READING_LISTS_LIMIT = 100;

    public static final int MIN_LANGUAGES_TO_UNLOCK_TRANSLATION = 2;

    public enum InvokeSource {
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
        LANG_VARIANT_DIALOG("lang_variant_dialog");

        private String name;

        InvokeSource(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum ImageEditType {
        ADD_CAPTION("addCaption"),
        ADD_CAPTION_TRANSLATION("addCaptionTranslation"),
        ADD_TAGS("addTags");

        private String name;

        ImageEditType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private Constants() { }
}
