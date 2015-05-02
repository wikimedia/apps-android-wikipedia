package org.wikipedia;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Represents a particular wiki.
 */
public class Site implements Parcelable {
    private final String domain;
    private final String language;

    public Site(String domain) {
        this(domain, domain.split("\\.")[0]);
    }
    public Site(String domain, String language) {
        this.domain = domain.replaceFirst("\\.m\\.", ".");
        this.language = language;
    }

    public Site(Parcel in) {
        this(in.readString());
    }

    public String getScriptPath(String script) {
        return "/w/" + script;
    }

    public String getResourceLoaderPath() {
        return getScriptPath("load.php");
    }

    public String getApiDomain() {
        return WikipediaApp.getInstance().getSslFallback() ? domain : domain.replaceFirst("\\.", ".m.");
    }

    public boolean getUseSecure() {
        return true;
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

    @Override
    public int hashCode() {
        return domain.hashCode();
    }

    @Override
    public String toString() {
        return "Site{"
                + "domain='" + domain + '\''
                + '}';
    }

    public String getFullUrl(String script) {
        return WikipediaApp.getInstance().getNetworkProtocol() + "://" + getDomain() + getScriptPath(script);
    }

    /**
     * Create a PageTitle object from an internal link string.
     *
     * @param internalLink Internal link target text (eg. /wiki/Target).
     *                     Should be URL decoded before passing in
     * @return A {@link PageTitle} object representing the internalLink passed in.
     */
    public PageTitle titleForInternalLink(String internalLink) {
        // FIXME: Handle language variant links properly
        // Strip the /wiki/ from the href
        return new PageTitle(internalLink.replaceFirst("/wiki/", ""), this);
    }

    /**
     * Create a PageTitle object from a Uri, taking into account any fragment (section title) in the link.
     * @param uri Uri object to be turned into a PageTitle.
     * @return {@link PageTitle} object that corresponds to the given Uri.
     */
    public PageTitle titleForUri(Uri uri) {
        String path = uri.getPath();
        if (!TextUtils.isEmpty(uri.getFragment())) {
            path += "#" + uri.getFragment();
        }
        return titleForInternalLink(path);
    }

    public String getLanguage() {
        return language;
    }

    public static Site forLang(String lang) {
        return new Site(lang + ".wikipedia.org", lang);
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
