package org.wikipedia;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.wikipedia.interlanguage.AppLanguageLookUpTable;
import org.wikipedia.page.PageTitle;

import java.util.Locale;

/**
 * The host domain and Wikipedia language code for a wiki site. Examples:
 *
 * <ul>
 *     <lh>Name: host / language code</lh>
 *     <li>English Wikipedia: en.wikipedia.org / en</li>
 *     <li>Chinese Wikipedia: zh.wikipedia.org / zh-hans or zh-hant</li>
 *     <li>Meta-Wiki: meta.wikimedia.org / meta</li>
 *     <li>Test Wikipedia: test.wikipedia.org / test</li>
 *     <li>Võro Wikipedia: fiu-vro.wikipedia.org / fiu-vro</li>
 * </ul>
 */
public class Site implements Parcelable {
    private final String domain;

    private final String languageCode; // or meta

    public Site(String domain) {
        this(domain, urlToLanguage(domain));
    }

    public Site(String domain, String languageCode) {
        this.domain = urlToDesktopSite(domain);
        this.languageCode = languageCode;
    }

    public Site(Parcel in) {
        this(in.readString(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(domain);
        dest.writeString(languageCode);
    }

    /**
     * @return A hostless path for the segment including a leading "/".
     */
    public String getScriptPath(String script) {
        return "/w/" + script;
    }

    /**
     * @return True if the URL scheme is secure. Examples:
     *
     * <ul>
     *     <lh>Scheme: return value</lh>
     *     <li>HTTPS: true</li>
     *     <li>HTTP: false</li>
     * </ul>
     */
    public boolean getUseSecure() {
        return true;
    }

    /**
     * @return The complete wiki host domain including language subdomain but not including scheme,
     *         authentication, port, nor trailing slash.
     *
     * @see <a href='https://en.wikipedia.org/wiki/Uniform_Resource_Locator#Syntax'>URL syntax</a>
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Like {@link #getDomain} but with a "m." between the language subdomain and the rest of the
     * host. Examples:
     *
     * <ul>
     *     <li>English Wikipedia: en.m.wikipedia.org</li>
     *     <li>Chinese Wikipedia: zh.m.wikipedia.org</li>
     *     <li>Meta-Wiki: meta.m.wikimedia.org</li>
     *     <li>Test Wikipedia: test.m.wikipedia.org</li>
     *     <li>Võro Wikipedia: fiu-vro.m.wikipedia.org</li>
     * </ul>
     */
    public String getMobileDomain() {
        return urlToMobileSite(domain);
    }

    @Override
    public int describeContents() {
        return 0;
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

    // Auto-generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Site site = (Site) o;

        if (domain != null ? !domain.equals(site.domain) : site.domain != null) {
            return false;
        }
        return !(languageCode != null ? !languageCode.equals(site.languageCode) : site.languageCode != null);

    }

    // Auto-generated
    @Override
    public int hashCode() {
        int result = domain != null ? domain.hashCode() : 0;
        result = 31 * result + (languageCode != null ? languageCode.hashCode() : 0);
        return result;
    }

    // Auto-generated
    @Override
    public String toString() {
        return "Site{"
                + "domain='" + domain + '\''
                + ", languageCode='" + languageCode + '\''
                + '}';
    }

    /**
     * @return The canonical URL for segment.
     */
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

    /**
     * @return The wiki language code, possibly a non-language such as meta or test, which may
     *         differ from the language subdomain.
     *
     * @see AppLanguageLookUpTable
     */
    public String getLanguageCode() {
        return languageCode;
    }

    public static Site forLanguage(String language) {
        return new Site(languageToWikiSubdomain(language) + ".wikipedia.org", language);
    }

    /**
     * @return True if the host is supported by the app.
     */
    public static boolean isSupportedSite(String domain) {
        return domain.matches("[a-z\\-]+\\.(m\\.)?wikipedia\\.org");
    }

    private static String urlToLanguage(String url) {
        return url.split("\\.")[0];
    }

    private String urlToDesktopSite(String url) {
        return url.replaceFirst("\\.m\\.", ".");
    }

    private String urlToMobileSite(String url) {
        return url.replaceFirst("\\.", ".m.");
    }

    private static String languageToWikiSubdomain(String language) {
        switch (language) {
            case AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE:
            case AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE:
                return Locale.CHINA.getLanguage();
            default:
                return language;
        }
    }
}