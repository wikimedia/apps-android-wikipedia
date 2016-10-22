package org.wikipedia.pageimages;

import android.os.Parcel;
import android.os.Parcelable;

import org.wikipedia.page.PageTitle;

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
}
