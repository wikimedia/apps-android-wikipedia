package org.wikipedia.dataclient;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.language.AppLanguageLookUpTable;
import org.wikipedia.language.LanguageUtil;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.UriUtil;

/**
 * The base URL and Wikipedia language code for a MediaWiki site. Examples:
 *
 * <ul>
 *     <lh>Name: scheme / authority / language code</lh>
 *     <li>English Wikipedia: HTTPS / en.wikipedia.org / en</li>
 *     <li>Chinese Wikipedia: HTTPS / zh.wikipedia.org / zh-hans or zh-hant</li>
 *     <li>Meta-Wiki: HTTPS / meta.wikimedia.org / (none)</li>
 *     <li>Test Wikipedia: HTTPS / test.wikipedia.org / test</li>
 *     <li>Võro Wikipedia: HTTPS / fiu-vro.wikipedia.org / fiu-vro</li>
 *     <li>Simple English Wikipedia: HTTPS / simple.wikipedia.org / simple</li>
 *     <li>Simple English Wikipedia (beta cluster mirror): HTTP / simple.wikipedia.beta.wmflabs.org / simple</li>
 *     <li>Development: HTTP / 192.168.1.11:8080 / (none)</li>
 * </ul>
 *
 * <strong>As shown above, the language code or mapping is part of the authority:</strong>
 * <ul>
 *     <lh>Validity: authority / language code</lh>
 *     <li>Correct: "test.wikipedia.org" / "test"</li>
 *     <li>Correct: "wikipedia.org", ""</li>
 *     <li>Correct: "no.wikipedia.org", "nb"</li>
 *     <li>Incorrect: "wikipedia.org", "test"</li>
 * </ul>
 */
public class WikiSite implements Parcelable {
    public static final String DEFAULT_SCHEME = "https";
    private static String DEFAULT_BASE_URL;

    public static final Parcelable.Creator<WikiSite> CREATOR = new Parcelable.Creator<WikiSite>() {
        @Override
        public WikiSite createFromParcel(Parcel in) {
            return new WikiSite(in);
        }

        @Override
        public WikiSite[] newArray(int size) {
            return new WikiSite[size];
        }
    };

    // todo: remove @SerializedName. this is now in the TypeAdapter and a "uri" case may be added
    @SerializedName("domain") @NonNull private final Uri uri;
    @NonNull private String languageCode;

    public static boolean supportedAuthority(@NonNull String authority) {
        return authority.endsWith(Uri.parse(DEFAULT_BASE_URL).getAuthority());
    }

    public static void setDefaultBaseUrl(@NonNull String url) {
        DEFAULT_BASE_URL = TextUtils.isEmpty(url) ? Service.WIKIPEDIA_URL : url;
    }

    public static WikiSite forLanguageCode(@NonNull String languageCode) {
        Uri uri = ensureScheme(Uri.parse(DEFAULT_BASE_URL));
        return new WikiSite((languageCode.isEmpty()
                ? "" : (languageCodeToSubdomain(languageCode) + ".")) + uri.getAuthority(),
                languageCode);
    }

    public WikiSite(@NonNull Uri uri) {
        Uri tempUri = ensureScheme(uri);
        String authority = StringUtils.defaultString(tempUri.getAuthority());
        if (("wikipedia.org".equals(authority) || "www.wikipedia.org".equals(authority))
                && tempUri.getPath() != null && tempUri.getPath().startsWith("/wiki")) {
            // Special case for Wikipedia only: assume English subdomain when none given.
            authority = "en.wikipedia.org";
        }

        // Unconditionally transform any mobile authority to canonical.
        authority = authority.replace(".m.", ".");

        String langVariant = UriUtil.getLanguageVariantFromUri(tempUri);
        if (!TextUtils.isEmpty(langVariant)) {
            languageCode = langVariant;
        } else {
            languageCode = authorityToLanguageCode(authority);
        }

        // This prevents showing mixed Chinese variants article when the URL is /zh/ or /wiki/ in zh.wikipedia.org
        if (languageCode.equals(AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE)) {
            languageCode = LanguageUtil.getFirstSelectedChineseVariant();
        }

        // Use default subdomain in authority to prevent error when requesting endpoints. e.g. zh-tw.wikipedia.org
        if (authority.contains("wikipedia.org") && !TextUtils.isEmpty(subdomain())) {
            authority = subdomain() + ".wikipedia.org";
        }

        this.uri = new Uri.Builder()
                .scheme(tempUri.getScheme())
                .encodedAuthority(authority)
                .build();
    }

    public WikiSite(@NonNull String url) {
        this(url.startsWith("http") ? Uri.parse(url) : url.startsWith("//")
                ? Uri.parse(DEFAULT_SCHEME + ":" + url) : Uri.parse(DEFAULT_SCHEME + "://" + url));
    }

    public WikiSite(@NonNull String authority, @NonNull String languageCode) {
        this(authority);
        this.languageCode = languageCode;
    }

    @NonNull
    public String scheme() {
        return TextUtils.isEmpty(uri.getScheme()) ? DEFAULT_SCHEME : uri.getScheme();
    }

    /**
     * @return The complete wiki authority including language subdomain but not including scheme,
     *         authentication, port, nor trailing slash.
     *
     * @see <a href='https://en.wikipedia.org/wiki/Uniform_Resource_Locator#Syntax'>URL syntax</a>
     */
    @NonNull
    public String authority() {
        return StringUtils.defaultString(uri.getAuthority());
    }

    @NonNull
    public String subdomain() {
        return languageCodeToSubdomain(languageCode);
    }

    /**
     * @return A path without an authority for the segment including a leading "/".
     */
    @NonNull
    public String path(@NonNull String segment) {
        return "/w/" + segment;
    }


    @NonNull public Uri uri() {
        return uri;
    }

    /**
     * @return The canonical URL. e.g., https://en.wikipedia.org.
     */
    @NonNull public String url() {
        return uri.toString();
    }

    /**
     * @return The canonical URL for segment. e.g., https://en.wikipedia.org/w/foo.
     */
    @NonNull public String url(@NonNull String segment) {
        return url() + path(segment);
    }

    /**
     * @return The wiki language code which may differ from the language subdomain. Empty if
     *         language code is unknown. Ex: "en", "zh-hans", ""
     *
     * @see AppLanguageLookUpTable
     */
    @NonNull
    public String languageCode() {
        return languageCode;
    }

    // TODO: this method doesn't have much to do with WikiSite. Move to PageTitle?
    /**
     * Create a PageTitle object from an internal link string.
     *
     * @param internalLink Internal link target text (eg. /wiki/Target).
     *                     Should be URL decoded before passing in
     * @return A {@link PageTitle} object representing the internalLink passed in.
     */
    public PageTitle titleForInternalLink(String internalLink) {
        // Strip the /wiki/ from the href
        return new PageTitle(UriUtil.removeInternalLinkPrefix(internalLink), this);
    }

    // TODO: this method doesn't have much to do with WikiSite. Move to PageTitle?
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

    @NonNull public String dbName() {
        return subdomain().replaceAll("-", "_") + "wiki";
    }

    // Auto-generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WikiSite wiki = (WikiSite) o;

        if (!uri.equals(wiki.uri)) {
            return false;
        }
        return languageCode.equals(wiki.languageCode);
    }

    // Auto-generated
    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + languageCode.hashCode();
        return result;
    }

    // Auto-generated
    @Override
    @NonNull
    public String toString() {
        return "WikiSite{"
                + "uri=" + uri
                + ", languageCode='" + languageCode + '\''
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(uri, 0);
        dest.writeString(languageCode);
    }

    protected WikiSite(@NonNull Parcel in) {
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.languageCode = in.readString();
    }

    @NonNull
    private static String languageCodeToSubdomain(@NonNull String languageCode) {
        return StringUtils.defaultString(WikipediaApp.getInstance().getAppLanguageState().getDefaultLanguageCode(languageCode), normalizeLanguageCode(languageCode));
    }

    @NonNull public static String normalizeLanguageCode(@NonNull String languageCode) {
        switch (languageCode) {
            case AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE:
                return AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE; // T114042
            case AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE:
                return AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE; // T111853
            default:
                return languageCode;
        }
    }

    @NonNull private static String authorityToLanguageCode(@NonNull String authority) {
        String[] parts = authority.split("\\.");
        final int minLengthForSubdomain = 3;
        if (parts.length < minLengthForSubdomain
                || parts.length == minLengthForSubdomain && parts[0].equals("m")) {
            // ""
            // wikipedia.org
            // m.wikipedia.org
            return "";
        }
        return parts[0];
    }

    @NonNull private static Uri ensureScheme(@NonNull Uri uri) {
        if (TextUtils.isEmpty(uri.getScheme())) {
            return uri.buildUpon().scheme(DEFAULT_SCHEME).build();
        }
        return uri;
    }
}
