package org.wikipedia.page;

import org.wikipedia.PageTitle;
import org.wikipedia.history.HistoryEntry;
import android.os.Parcel;
import android.os.Parcelable;

public class PageBackStackItem implements Parcelable {
    private final PageTitle title;
    public PageTitle getTitle() {
        return title;
    }

    private final HistoryEntry historyEntry;
    public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    private int scrollY;
    public int getScrollY() {
        return scrollY;
    }
    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    public PageBackStackItem(PageTitle title, HistoryEntry historyEntry) {
        this.title = title;
        this.historyEntry = historyEntry;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(title, flags);
        dest.writeParcelable(historyEntry, flags);
        dest.writeInt(scrollY);
    }

    private PageBackStackItem(Parcel in) {
        title = in.readParcelable(PageTitle.class.getClassLoader());
        historyEntry = in.readParcelable(HistoryEntry.class.getClassLoader());
        scrollY = in.readInt();
    }

    public static final Parcelable.Creator<PageBackStackItem> CREATOR
            = new Parcelable.Creator<PageBackStackItem>() {
        public PageBackStackItem createFromParcel(Parcel in) {
            return new PageBackStackItem(in);
        }

        public PageBackStackItem[] newArray(int size) {
            return new PageBackStackItem[size];
        }
    };
}
