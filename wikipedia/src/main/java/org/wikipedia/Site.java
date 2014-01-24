package org.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.*;

/**
 * Represents a particular Wikimedia project.
 */
public class Site implements Parcelable {
    private final String domain;

    public Site(String domain) {
        this.domain = domain;
    }

    protected String getApiDomain() {
        return domain.replaceFirst("\\.", ".m.");
    }

    public Site(Parcel in) {
        domain = in.readString();
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(domain);
    }

    public static final Parcelable.Creator<Site> CREATOR
            = new Parcelable.Creator<Site>() {
        public Site createFromParcel(Parcel in) {
            return new Site(in);
        }

        public Site[] newArray(int size) {
            return new Site[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Site)) {
            return false;
        }

        return ((Site)o).getDomain().equals(domain);
    }

    public String getFullUrl(String path) {
        return WikipediaApp.PROTOCOL + "://" + getDomain() + path;
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
    public PageTitle titleForInternalLink(String internalLink) {
        //TODO: Do better validation of internal links!
        //TODO: Handle fragments better!
        Matcher matches = internalLinkMatchPattern.matcher(internalLink);
        if (matches.matches()) {
            try {
                String namespace = matches.group(1) != null ? URLDecoder.decode(matches.group(1), "utf-8") : null;
                String pageText = URLDecoder.decode(matches.group(2), "utf-8");
                return new PageTitle(namespace, pageText, this);
            } catch (UnsupportedEncodingException e) {
                // NOT HAPPENING! JESUS CHRIST JAVA!
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Did not  match internalLinkPattern: " + internalLink);
        }

    }

    private String language;
    public String getLanguage() {
        if (language == null) {
            language = domain.split("\\.")[0];
        }
        return language;
    }

    public static Site forLang(String lang) {
        return new Site(lang + ".wikipedia.org");
    }

    /**
     * Returns if the site is supported
     * @param domain the site domain
     * @return boolean
     */
    public static boolean isSupportedSite(String domain) {
        return domain.matches("[a-z\\-]+\\.(m\\.)?wikipedia\\.org");
    }
}
