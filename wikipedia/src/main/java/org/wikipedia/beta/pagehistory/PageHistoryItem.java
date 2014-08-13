package org.wikipedia.beta.pagehistory;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.beta.PageTitle;

import java.util.Date;

public class PageHistoryItem implements Parcelable {
    private final String username;
    private final Date timestamp;
    private final String summary;
    private final int sizeDiff;
    private final PageTitle title;

    public String getUsername() {
        return username;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getSummary() {
        return summary;
    }

    public int getSizeDiff() {
        return sizeDiff;
    }

    public PageTitle getTitle() {
        return title;
    }

    public PageHistoryItem(String username, Date timestamp, String summary, int sizeDiff, PageTitle title) {
        this.username = username;
        this.timestamp = timestamp;
        this.summary = summary;
        this.sizeDiff = sizeDiff;
        this.title = title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(username);
        parcel.writeSerializable(timestamp);
        parcel.writeString(summary);
        parcel.writeInt(sizeDiff);
        parcel.writeParcelable(title, flags);
    }

    private PageHistoryItem(Parcel in) {
        this.username = in.readString();
        this.timestamp = (Date) in.readSerializable();
        this.summary = in.readString();
        this.sizeDiff = in.readInt();
        this.title = in.readParcelable(PageTitle.class.getClassLoader());
    }

    public static final Parcelable.Creator<PageHistoryItem> CREATOR
            = new Parcelable.Creator<PageHistoryItem>() {
        public PageHistoryItem createFromParcel(Parcel in) {
            return new PageHistoryItem(in);
        }

        public PageHistoryItem[] newArray(int size) {
            return new PageHistoryItem[size];
        }
    };
}
