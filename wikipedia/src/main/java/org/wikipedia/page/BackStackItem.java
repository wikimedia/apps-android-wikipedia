package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.PageTitle;
import org.wikipedia.history.HistoryEntry;

public class BackStackItem implements Parcelable {
    final PageTitle title;
    final HistoryEntry historyEntry;
    final int scrollPosition;

    public BackStackItem(PageTitle title, HistoryEntry historyEntry, int scrollPosition) {
        this.title = title;
        this.historyEntry = historyEntry;
        this.scrollPosition = scrollPosition;
    }

    public BackStackItem(Parcel in) {
        title = in.readParcelable(PageTitle.class.getClassLoader());
        historyEntry = in.readParcelable(HistoryEntry.class.getClassLoader());
        scrollPosition = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(title, flags);
        parcel.writeParcelable(historyEntry, flags);
        parcel.writeInt(scrollPosition);
    }

    public static final Parcelable.Creator<BackStackItem> CREATOR
            = new Parcelable.Creator<BackStackItem>() {
        public BackStackItem createFromParcel(Parcel in) {
            return new BackStackItem(in);
        }

        public BackStackItem[] newArray(int size) {
            return new BackStackItem[size];
        }
    };
}
