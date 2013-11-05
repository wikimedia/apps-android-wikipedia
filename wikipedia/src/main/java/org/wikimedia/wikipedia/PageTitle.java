package org.wikimedia.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Immutable value object representing the text of a page.
 *
 * Points to a specific page in a specific namespace on a specific site.
 * Is immutable.
 */
public class PageTitle implements Parcelable {
    private final String namespace;
    private final String text;

    public PageTitle(final String namespace, final String text) {
        this.namespace = namespace;
        this.text = text;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getText() {
        return text;
    }

    public String getPrefixedText() {
        return namespace == null ? text : namespace + ":" + text;
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
        text = in.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(text);
    }
}
