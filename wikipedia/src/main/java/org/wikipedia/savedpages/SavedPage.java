package org.wikipedia.savedpages;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.PageTitle;

import java.util.Date;

public class SavedPage implements Parcelable {
    public static final SavedPagePersistanceHelper PERSISTANCE_HELPER = new SavedPagePersistanceHelper();

    private final PageTitle title;
    private final Date timestamp;

    public SavedPage(PageTitle title, Date timestamp) {
        this.title = title;
        this.timestamp = timestamp;
    }

    public SavedPage(PageTitle title) {
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
        if (!(o instanceof SavedPage)) {
            return false;
        }
        SavedPage other = (SavedPage) o;
        return title.equals(other.title);
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }

    @Override
    public String toString() {
        return "SavedPage{"
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

    private SavedPage(Parcel in) {
        this.title = in.readParcelable(PageTitle.class.getClassLoader());
        this.timestamp = new Date(in.readLong());
    }

    public static final Creator<SavedPage> CREATOR
            = new Creator<SavedPage>() {
        public SavedPage createFromParcel(Parcel in) {
            return new SavedPage(in);
        }

        public SavedPage[] newArray(int size) {
            return new SavedPage[size];
        }
    };
}
