package org.wikipedia.search;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class RecentSearch implements Parcelable {
    public static final RecentSearchDatabaseTable DATABASE_TABLE = new RecentSearchDatabaseTable();

    private final String text;
    private final Date timestamp;

    public RecentSearch(String text) {
        this(text, new Date());
    }

    public RecentSearch(String text, Date timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RecentSearch)) {
            return false;
        }
        RecentSearch other = (RecentSearch) o;
        return text.equals(other.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "RecentSearch{"
                + "text=" + text
                + ", timestamp=" + timestamp.getTime()
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getText());
        dest.writeLong(getTimestamp().getTime());
    }

    private RecentSearch(Parcel in) {
        this.text = in.readString();
        this.timestamp = new Date(in.readLong());
    }

    public static final Creator<RecentSearch> CREATOR
            = new Creator<RecentSearch>() {
        @Override
        public RecentSearch createFromParcel(Parcel in) {
            return new RecentSearch(in);
        }

        @Override
        public RecentSearch[] newArray(int size) {
            return new RecentSearch[size];
        }
    };
}
