package org.wikipedia.pageimages;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.page.PageTitle;

import java.util.List;
import java.util.Map;

public class PageImage implements Parcelable {
    public static final PageImageDatabaseTable DATABASE_TABLE = new PageImageDatabaseTable();

    private final PageTitle title;
    private final String imageName;

    public PageImage(PageTitle title, String imageName) {
        this.title = title;
        this.imageName = imageName;
    }

    public PageTitle getTitle() {
        return title;
    }

    public String getImageName() {
        return imageName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageImage)) {
            return false;
        }
        PageImage other = (PageImage) o;
        return title.equals(other.title)
                && imageName.equals(other.imageName);
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + imageName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PageImage{"
                + "title=" + title
                + ", imageName='" + imageName + '\''
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(getTitle(), flags);
        dest.writeString(getImageName());
    }

    private PageImage(Parcel in) {
        this.title = in.readParcelable(PageTitle.class.getClassLoader());
        this.imageName = in.readString();
    }

    public static final Parcelable.Creator<PageImage> CREATOR
            = new Parcelable.Creator<PageImage>() {
        @Override
        public PageImage createFromParcel(Parcel in) {
            return new PageImage(in);
        }

        @Override
        public PageImage[] newArray(int size) {
            return new PageImage[size];
        }
    };

    public static Map<PageTitle, PageImage> imageMapFromPages(@NonNull WikiSite wiki, @NonNull List<PageTitle> titles, @NonNull List<MwQueryPage> pages) {
        Map<PageTitle, PageImage> pageImagesMap = new ArrayMap<>();
        // nominal case
        Map<String, PageTitle> titlesMap = new ArrayMap<>();
        for (PageTitle title : titles) {
            titlesMap.put(title.getPrefixedText(), title);
        }
        Map<String, String> thumbnailSourcesMap = new ArrayMap<>();
        Map<String, String> convertedTitleMap = new ArrayMap<>();

        // noinspection ConstantConditions
        for (MwQueryPage page : pages) {
            thumbnailSourcesMap.put(new PageTitle(null, page.title(), wiki).getPrefixedText(), page.thumbUrl());
            if (!TextUtils.isEmpty(page.convertedFrom())) {
                PageTitle pageTitle = new PageTitle(null, page.convertedFrom(), wiki);
                convertedTitleMap.put(pageTitle.getPrefixedText(), page.convertedTo());
                thumbnailSourcesMap.put(pageTitle.getPrefixedText(), page.thumbUrl());
            }
            if (!TextUtils.isEmpty(page.redirectFrom())) {
                thumbnailSourcesMap.put(new PageTitle(null, page.redirectFrom(), wiki).getPrefixedText(), page.thumbUrl());
            }
        }

        for (String key : titlesMap.keySet()) {
            if (thumbnailSourcesMap.containsKey(key)) {
                PageTitle title = titlesMap.get(key);
                title.setConvertedText(convertedTitleMap.get(key));
                pageImagesMap.put(title, new PageImage(title, thumbnailSourcesMap.get(key)));
            }
        }
        return pageImagesMap;
    }
}
