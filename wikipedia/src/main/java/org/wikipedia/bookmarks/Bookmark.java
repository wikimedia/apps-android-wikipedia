package org.wikipedia.bookmarks;

import android.os.*;
import org.wikipedia.*;

import java.util.*;

public class Bookmark implements Parcelable {
    public static final BookmarkPersistanceHelper PERSISTANCE_HELPER = new BookmarkPersistanceHelper();

    private final PageTitle title;
    private final Date timestamp;

    public Bookmark(PageTitle title, Date timestamp) {
        this.title = title;
        this.timestamp = timestamp;
    }

    public Bookmark(PageTitle title) {
        this(title, new Date());
    }

    public PageTitle getTitle() {
        return title;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Bookmark)) {
            return false;
        }
        Bookmark other = (Bookmark) o;
        return title.equals(other.title);
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }

    @Override
    public String toString() {
        return "Bookmark{"
                + "title=" + title
                + ", timestamp=" + timestamp.getTime()
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(getTitle(), flags);
        dest.writeLong(getTimestamp().getTime());
    }

    private Bookmark(Parcel in) {
        this.title = in.readParcelable(PageTitle.class.getClassLoader());
        this.timestamp = new Date(in.readLong());
    }

    public static final Creator<Bookmark> CREATOR
            = new Creator<Bookmark>() {
        public Bookmark createFromParcel(Parcel in) {
            return new Bookmark(in);
        }

        public Bookmark[] newArray(int size) {
            return new Bookmark[size];
        }
    };
}
