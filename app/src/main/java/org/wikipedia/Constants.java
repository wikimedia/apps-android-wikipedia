package org.wikipedia;

public final class Constants {
    // Keep loader IDs unique to each loader. If the loader specified by the ID already exists, the
    // last created loader is reused.
    public static final int HISTORY_FRAGMENT_LOADER_ID = 100;
    public static final int RECENT_SEARCHES_FRAGMENT_LOADER_ID = 101;

    public static final String WIKIPEDIA_URL = "https://wikipedia.org/";
    public static final String PLAIN_TEXT_MIME_TYPE = "text/plain";

    public static final String ACCEPT_HEADER_PREFIX = "accept: application/json; charset=utf-8; "
            + "profile=\"https://www.mediawiki.org/wiki/Specs/";
    public static final String ACCEPT_HEADER_SUMMARY = ACCEPT_HEADER_PREFIX + "Summary/1.2.0\"";

    public static final int ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 44;
    public static final int ACTIVITY_REQUEST_VOICE_SEARCH = 45;
    public static final int ACTIVITY_REQUEST_LANGLINKS = 50;
    public static final int ACTIVITY_REQUEST_EDIT_SECTION = 51;
    public static final int ACTIVITY_REQUEST_GALLERY = 52;
    public static final int ACTIVITY_REQUEST_LOGIN = 53;
    public static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS = 54;
    public static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT = 55;
    public static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL = 56;
    public static final int ACTIVITY_REQUEST_OFFLINE_TUTORIAL = 57;
    public static final int ACTIVITY_REQUEST_FEED_CONFIGURE = 58;

    public static final String INTENT_RETURN_TO_MAIN = "returnToMain";
    public static final String INTENT_SEARCH_FROM_WIDGET = "searchFromWidget";
    public static final String INTENT_FEATURED_ARTICLE_FROM_WIDGET = "featuredArticleFromWidget";

    public static final String INTENT_APP_SHORTCUT_SEARCH = "appShortcutSearch";
    public static final String INTENT_APP_SHORTCUT_CONTINUE_READING = "appShortcutContinueReading";
    public static final String INTENT_APP_SHORTCUT_RANDOM = "appShortcutRandom";

    public static final String INTENT_EXTRA_REVERT_QNUMBER = "revertQNumber";
    public static final String INTENT_EXTRA_DELETE_READING_LIST = "deleteReadingList";

    public static final String INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME = "syncPauseResume";
    public static final String INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL = "syncCancel";

    public static final int PROGRESS_BAR_MAX_VALUE = 10_000;

    public static final int MAX_SUGGESTION_RESULTS = 3;
    public static final int SUGGESTION_REQUEST_ITEMS = 5;
    public static final int API_QUERY_MAX_TITLES = 50;

    public static final int PREFERRED_THUMB_SIZE = 320;

    public static final int MAX_TABS = 100;
    public static final int MAX_READING_LIST_ARTICLE_LIMIT = 5000;
    public static final int MAX_READING_LISTS_LIMIT = 100;

    private Constants() { }
}
