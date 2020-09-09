package org.wikipedia.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.PageTitle;

public class SearchResult extends BaseModel implements Parcelable {
    private final PageTitle pageTitle;
    private final String redirectFrom;
    private final SearchResultType searchResultType;

    public enum SearchResultType {
        SEARCH,
        HISTORY,
        READING_LIST,
        TAB_LIST
    }

    public SearchResult(@NonNull MwQueryPage page, @NonNull WikiSite wiki) {
        this(new PageTitle(page.title(), wiki, page.thumbUrl(), page.description(), page.displayTitle(wiki.languageCode())), page.redirectFrom(), SearchResultType.SEARCH);
    }

    public SearchResult(@NonNull PageTitle pageTitle, @NonNull SearchResultType searchResultType) {
        this(pageTitle, null, searchResultType);
    }

    public SearchResult(@NonNull PageTitle pageTitle, @Nullable String redirectFrom, @NonNull SearchResultType searchResultType) {
        this.pageTitle = pageTitle;
        this.redirectFrom = redirectFrom;
        this.searchResultType = searchResultType;
    }

    public SearchResultType getType() {
        return searchResultType;
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
        parcel.writeSerializable(searchResultType);
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
        searchResultType = (SearchResultType) in.readSerializable();
    }
}

