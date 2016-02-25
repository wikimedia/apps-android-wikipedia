package org.wikipedia;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.interlanguage.AppLanguageLookUpTable;
import org.wikipedia.page.PageTitle;

import java.util.Locale;

/**
 * The host host and Wikipedia language code for a wiki site. Examples:
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
    @SerializedName("domain")
    private final String host;

    private final String languageCode; // or meta

    public Site(String host) {
        this(host, hostToLanguageCode(host));
    }

    public Site(String host, String languageCode) {
        this.host = hostToDesktop(host);
        this.languageCode = languageCode;
    }

    public Site(Parcel in) {
        this(in.readString(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeString(languageCode);
    }

    /**
     * @return A hostless path for the segment including a leading "/".
     */
    public String path(String segment) {
        return "/w/" + segment;
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
    public boolean secureScheme() {
        // TODO: unify with WikipediaApp.getNetworkProtocol().
        return true;
    }

    /**
     * @return The complete wiki host host including language subdomain but not including scheme,
     *         authentication, port, nor trailing slash.
     *
     * @see <a href='https://en.wikipedia.org/wiki/Uniform_Resource_Locator#Syntax'>URL syntax</a>
     */
    public String host() {
        return host;
    }

    /**
     * Like {@link #host} but with a "m." between the language subdomain and the rest of the host.
     * Examples:
     *
     * <ul>
     *     <li>English Wikipedia: en.m.wikipedia.org</li>
     *     <li>Chinese Wikipedia: zh.m.wikipedia.org</li>
     *     <li>Meta-Wiki: meta.m.wikimedia.org</li>
     *     <li>Test Wikipedia: test.m.wikipedia.org</li>
     *     <li>Võro Wikipedia: fiu-vro.m.wikipedia.org</li>
     * </ul>
     */
    public String mobileHost() {
        return hostToMobile(host);
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

        if (host != null ? !host.equals(site.host) : site.host != null) {
            return false;
        }
        return !(languageCode != null ? !languageCode.equals(site.languageCode) : site.languageCode != null);

    }

    // Auto-generated
    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (languageCode != null ? languageCode.hashCode() : 0);
        return result;
    }

    // Auto-generated
    @Override
    public String toString() {
        return "Site{"
                + "host='" + host + '\''
                + ", languageCode='" + languageCode + '\''
                + '}';
    }

    /**
     * @return The canonical URL for segment.
     */
    public String url(String segment) {
        return WikipediaApp.getInstance().getNetworkProtocol() + "://" + host() + path(segment);
    }

    // TODO: this method doesn't have much to do with Site. Move to PageTitle?
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

    // TODO: this method doesn't have much to do with Site. Move to PageTitle?
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
    public String languageCode() {
        return languageCode;
    }

    public static Site forLanguageCode(String languageCode) {
        // TODO: this host assumption won't work for custom domains like meta, the Wikipedia beta
        //       cluster, and Vagrant instances.
        return new Site(languageCodeToSubdomain(languageCode) + ".wikipedia.org", languageCode);
    }

    /**
     * @return True if the host is supported by the app.
     */
    public static boolean supportedHost(String host) {
        // TODO: this host assumption won't work for custom domains like meta, the Wikipedia beta
        //       cluster, and Vagrant instances.
        return host.matches("[a-z\\-]+\\.(m\\.)?wikipedia\\.org");
    }

    private static String hostToLanguageCode(String host) {
        return host.split("\\.")[0];
    }

    private String hostToDesktop(String host) {
        return host.replaceFirst("\\.m\\.", ".");
    }

    private String hostToMobile(String host) {
        return host.replaceFirst("\\.", ".m.");
    }

    private static String languageCodeToSubdomain(String languageCode) {
        switch (languageCode) {
            case AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE:
            case AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE:
                return Locale.CHINA.getLanguage();
            default:
                return languageCode;
        }
    }
}