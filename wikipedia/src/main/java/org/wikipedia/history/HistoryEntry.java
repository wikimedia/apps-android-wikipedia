package org.wikipedia.history;

import android.os.*;
import org.wikipedia.*;

import java.util.*;

public class HistoryEntry implements Parcelable {
    public static final HistoryEntryPersistanceHelper PERSISTANCE_HELPER = new HistoryEntryPersistanceHelper();

    public static final int SOURCE_SEARCH = 1;
    public static final int SOURCE_INTERNAL_LINK = 2;
    public static final int SOURCE_EXTERNAL_LINK = 3;
    public static final int SOURCE_HISTORY = 4;
    public static final int SOURCE_SAVED_PAGE = 5;
    public static final int SOURCE_LANGUAGE_LINK = 6;
    public static final int SOURCE_RANDOM = 7;

    private final PageTitle title;
    private final Date timestamp;
    private final int source;

    public HistoryEntry(PageTitle title, Date timestamp, int source) {
        this.title = title;
        this.timestamp = timestamp;
        this.source = source;
    }

    public HistoryEntry(PageTitle title, int source) {
        this(title, new Date(), source);
    }

    public PageTitle getTitle() {
        return title;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HistoryEntry)) {
            return false;
        }
        HistoryEntry other = (HistoryEntry) o;
        return getTitle().equals(other.getTitle())
                && getTimestamp().equals(other.getTimestamp())
                && getSource() == other.getSource();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(getTitle(), flags);
        dest.writeLong(getTimestamp().getTime());
        dest.writeInt(getSource());
    }

    private HistoryEntry(Parcel in) {
        this.title = in.readParcelable(PageTitle.class.getClassLoader());
        this.timestamp = new Date(in.readLong());
        this.source = in.readInt();
    }

    public static final Parcelable.Creator<HistoryEntry> CREATOR
            = new Parcelable.Creator<HistoryEntry>() {
        public HistoryEntry createFromParcel(Parcel in) {
            return new HistoryEntry(in);
        }

        public HistoryEntry[] newArray(int size) {
            return new HistoryEntry[size];
        }
    };
}
