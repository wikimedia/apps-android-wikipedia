package org.wikipedia.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.PageTitle;

public class SearchResult extends BaseModel implements Parcelable {
    private PageTitle pageTitle;
    private final String redirectFrom;

    public SearchResult(@NonNull MwQueryPage page, @NonNull WikiSite wiki) {
        this(new PageTitle(page.title(), wiki, page.thumbUrl(), page.description()), page.redirectFrom());
    }

    public SearchResult(@NonNull PageTitle pageTitle) {
        this(pageTitle, null);
    }

    public SearchResult(@NonNull PageTitle pageTitle, @Nullable String redirectFrom) {
        this.pageTitle = pageTitle;
        this.redirectFrom = redirectFrom;
    }

    @NonNull
    public PageTitle getPageTitle() {
        return pageTitle;
    }

    @Nullable
    public String getRedirectFrom() {
        return redirectFrom;
    }

    @Override
    public String toString() {
        return pageTitle.getPrefixedText();
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
        parcel.writeParcelable(pageTitle, flags);
        parcel.writeString(redirectFrom);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchResult)) {
            return false;
        }
        SearchResult other = (SearchResult)o;
        return other.getPageTitle().equals(pageTitle) && TextUtils.equals(other.getRedirectFrom(), redirectFrom);
    }

    @Override
    public int hashCode() {
        int result = pageTitle.hashCode();
        result = 31 * result + redirectFrom.hashCode();
        return result;
    }

    private SearchResult(Parcel in) {
        pageTitle = in.readParcelable(PageTitle.class.getClassLoader());
        redirectFrom = in.readString();
    }
}

