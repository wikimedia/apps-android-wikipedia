package org.wikipedia.offline;

public class OfflineObjectDbHelper {
    private static OfflineObjectDbHelper INSTANCE;

    public static OfflineObjectDbHelper instance() {
        if (INSTANCE == null) {
            INSTANCE = new OfflineObjectDbHelper();
        }
        return INSTANCE;
    }

    
}
