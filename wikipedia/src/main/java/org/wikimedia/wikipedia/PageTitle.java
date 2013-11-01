package org.wikimedia.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Immutable value object representing the title of a page.
 *
 * Points to a specific page in a specific namespace on a specific site.
 * Is immutable.
 */
public class PageTitle implements Parcelable {
    private final String namespace;
    private final String title;

    public PageTitle(final String namesapce, final String title) {
        this.namespace = namesapce;
        this.title = title; //FIXME: Actually normalize this!
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PageTitle> CREATOR
            = new Parcelable.Creator<PageTitle>() {
        public PageTitle createFromParcel(Parcel in) {
            return new PageTitle(in);
        }

        public PageTitle[] newArray(int size) {
            return new PageTitle[size];
        }
    };

    private PageTitle(Parcel in) {
        namespace = in.readString();
        title = in.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(title);
    }
}
