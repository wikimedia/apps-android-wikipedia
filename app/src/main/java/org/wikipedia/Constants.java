package org.wikipedia;

public final class Constants {
    // Keep loader IDs unique to each loader. If the loader specified by the ID already exists, the
    // last created loader is reused.
    public static final int HISTORY_FRAGMENT_LOADER_ID = 100;
    public static final int SAVED_PAGES_FRAGMENT_LOADER_ID = 101;
    public static final int RECENT_SEARCHES_FRAGMENT_LOADER_ID = 102;
    public static final int USER_OPTION_ROW_FRAGMENT_LOADER_ID = 103;

    private Constants() { }
}