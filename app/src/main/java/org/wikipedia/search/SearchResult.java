package org.wikipedia.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.page.PageTitle;

public class SearchResult implements Parcelable {
    private final PageTitle title;
    private final String redirectFrom;

    public SearchResult(@NonNull PageTitle title) {
        this(title, null);
    }

    public SearchResult(@NonNull PageTitle title, @Nullable String redirectFrom) {
        this.title = title;
        this.redirectFrom = redirectFrom;
    }

    @NonNull
    public PageTitle getTitle() {
        return title;
    }

    @Nullable
    public String getRedirectFrom() {
        return redirectFrom;
    }

    @Override
    public String toString() {
        return title.getPrefixedText();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<SearchResult> CREATOR
            = new Parcelable.Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(title, flags);
        parcel.writeString(redirectFrom);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchResult)) {
            return false;
        }
        SearchResult other = (SearchResult)o;
        return other.getTitle().equals(title) && TextUtils.equals(other.getRedirectFrom(), redirectFrom);
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + redirectFrom.hashCode();
        return result;
    }

    private SearchResult(Parcel in) {
        title = in.readParcelable(PageTitle.class.getClassLoader());
        redirectFrom = in.readString();
    }
}

