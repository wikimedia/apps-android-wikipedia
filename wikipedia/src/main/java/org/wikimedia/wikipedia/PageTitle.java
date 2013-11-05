package org.wikimedia.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Regex to match internal relative wikilinks.
     *
     * Start with /wiki/, can contain a Namespace indicator and a fragment.
     * Capturing Group 1 is namespace (or null for no namespace). 2 is Page Title text. 3 is Fragment.
     */
    public static Pattern internalLinkMatchPattern = Pattern.compile("/wiki/(?:([^:]+):)?([^#]*)(?:#(.+))?");

    /**
     * Create a PageTitle object from an internal link string.
     *
     * @param internalLink Internal link target text (eg. /wiki/Target).
     *                     Should be URL decoded before passing in
     * @return A {@link PageTitle} object representing the internalLink passed in.
     */
    public static PageTitle fromInternalLink(String internalLink) {
        //TODO: Do better validation of internal links!
        //TODO: Handle fragments better!
        Matcher matches = internalLinkMatchPattern.matcher(internalLink);
        if (matches.matches()) {
            String namespace = matches.group(1);
            String pageText = matches.group(2);
            return new PageTitle(namespace, pageText);
        } else {
            throw new RuntimeException("Did not  match internalLinkPattern: " + internalLink);
        }

    }
}
