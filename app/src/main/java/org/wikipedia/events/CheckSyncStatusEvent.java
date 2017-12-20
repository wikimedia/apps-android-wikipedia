package org.wikipedia.events;

import android.support.v7.preference.Preference;

public class CheckSyncStatusEvent {
    private boolean syncDisable;
    private Preference preferenceOfSyncReadingLists;

    public CheckSyncStatusEvent() {
        this.syncDisable = false;
    }

    public CheckSyncStatusEvent(boolean disable) {
        setSyncDisable(disable);
    }

    public CheckSyncStatusEvent(Preference preference) {
       setPreferenceOfSyncReadingLists(preference);
    }

    private void setSyncDisable(boolean disable) {
        this.syncDisable = disable;
    }

    public boolean isSyncDisable() {
        return syncDisable;
    }

    private void setPreferenceOfSyncReadingLists(Preference preference) {
        this.preferenceOfSyncReadingLists = preference;
    }

    public Preference getPreferenceOfSyncReadingLists() {
        return preferenceOfSyncReadingLists;
    }
}
