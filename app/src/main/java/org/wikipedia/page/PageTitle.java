package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Locale;

import static org.wikipedia.util.UriUtil.decodeURL;

/**
 * Represents certain vital information about a page, including the title, namespace,
 * and fragment (section anchor target).  It can also contain a thumbnail URL for the
 * page, and a short description retrieved from Wikidata.
 *
 * WARNING: This class is not immutable! Specifically, the thumbnail URL and the Wikidata
 * description can be altered after construction. Therefore do NOT rely on all the fields
 * of a PageTitle to remain constant for the lifetime of the object.
 */
public class PageTitle implements Parcelable {

    public static final Parcelable.Creator<PageTitle> CREATOR
            = new Parcelable.Creator<PageTitle>() {
        @Override
        public PageTitle createFromParcel(Parcel in) {
            return new PageTitle(in);
        }

        @Override
        public PageTitle[] newArray(int size) {
            return new PageTitle[size];
        }
    };

    /**
     * The localised namespace of the page as a string, or null if the page is in mainspace.
     *
     * This field contains the prefix of the page's title, as opposed to the namespace ID used by
     * MediaWiki. Therefore, mainspace pages always have a null namespace, as they have no prefix,
     * and the namespace of a page will depend on the language of the wiki the user is currently
     * looking at.
     *
     * Examples:
     * * [[Manchester]] on enwiki will have a namespace of null
     * * [[Deutschland]] on dewiki will have a namespace of null
     * * [[User:Deskana]] on enwiki will have a namespace of "User"
     * * [[Utilisateur:Deskana]] on frwiki will have a namespace of "Utilisateur", even if you got
     *   to the page by going to [[User:Deskana]] and having MediaWiki automatically redirect you.
     */
    // TODO: remove. This legacy code is the localized namespace name (File, Special, Talk, etc) but
    //       isn't consistent across titles. e.g., articles with colons, such as RTÉ News: Six One,
    //       are broken.
    @Nullable private final String namespace;
    @NonNull private final String text;
    @Nullable private final String fragment;
    @Nullable private String thumbUrl;
    @SerializedName("site") @NonNull private final WikiSite wiki;
    @Nullable private String description;
    @Nullable private final PageProperties properties;

    /**
     * Creates a new PageTitle object.
     * Use this if you want to pass in a fragment portion separately from the title.
     *
     * @param prefixedText title of the page with optional namespace prefix
     * @param fragment optional fragment portion
     * @param wiki the wiki site the page belongs to
     * @return a new PageTitle object matching the given input parameters
     */
    public static PageTitle withSeparateFragment(@NonNull String prefixedText,
                                                 @Nullable String fragment, @NonNull WikiSite wiki) {
        if (TextUtils.isEmpty(fragment)) {
            return new PageTitle(prefixedText, wiki, null, (PageProperties) null);
        } else {
            // TODO: this class needs some refactoring to allow passing in a fragment
            // without having to do string manipulations.
            return new PageTitle(prefixedText + "#" + fragment, wiki, null, (PageProperties) null);
        }
    }

    public PageTitle(@Nullable final String namespace, @NonNull String text, @Nullable String fragment, @Nullable String thumbUrl, @NonNull WikiSite wiki) {
        this.namespace = namespace;
        this.text = text;
        this.fragment = fragment;
        this.wiki = wiki;
        this.thumbUrl = thumbUrl;
        properties = null;
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl, @Nullable String description, @Nullable PageProperties properties) {
        this(text, wiki, thumbUrl, properties);
        this.description = description;
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl, @Nullable String description) {
        this(text, wiki, thumbUrl);
        this.description = description;
    }

    public PageTitle(@Nullable String namespace, @NonNull String text, @NonNull WikiSite wiki) {
        this(namespace, text, null, null, wiki);
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl) {
        this(text, wiki, thumbUrl, (PageProperties) null);
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki) {
        this(text, wiki, null);
    }

    private PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl,
                      @Nullable PageProperties properties) {
        // FIXME: Does not handle mainspace articles with a colon in the title well at all
        if (TextUtils.isEmpty(text)) {
            // If empty, this refers to the main page.
            text = MainPageNameData.valueFor(wiki.languageCode());
        }

        String[] fragParts = text.split("#", -1);
        text = fragParts[0];
        if (fragParts.length > 1) {
            this.fragment = decodeURL(fragParts[1]).replace(" ", "_");
        } else {
            this.fragment = null;
        }

        String[] parts = text.split(":", -1);
        if (parts.length > 1) {
            String namespaceOrLanguage = parts[0];
            if (Arrays.asList(Locale.getISOLanguages()).contains(namespaceOrLanguage)) {
                this.namespace = null;
                this.wiki = WikiSite.forLanguageCode(namespaceOrLanguage);
            } else {
                this.wiki = wiki;
                this.namespace = namespaceOrLanguage;
            }
            this.text = TextUtils.join(":", Arrays.copyOfRange(parts, 1, parts.length));
        } else {
            this.wiki = wiki;
            this.namespace = null;
            this.text = parts[0];
        }

        this.thumbUrl = thumbUrl;
        this.properties = properties;
    }

    public PageTitle(JSONObject json) {
        this.namespace = json.optString("namespace", null);
        this.text = json.optString("text", null);
        this.fragment = json.optString("fragment", null);
        if (json.has("site")) {
            wiki = new WikiSite(json.optString("site"), json.optString("languageCode"));
        } else {
            L.logRemoteErrorIfProd(new RemoteLogException("wiki is null").put("json", json.toString()));
            wiki = WikipediaApp.getInstance().getWikiSite();
        }
        this.properties = json.has("properties") ? new PageProperties(json.optJSONObject("properties")) : null;
        this.thumbUrl = json.optString("thumbUrl", null);
        this.description = json.optString("description", null);
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @NonNull public Namespace namespace() {
        if (properties != null) {
            return properties.getNamespace();
        }

        // Properties has the accurate namespace but it doesn't exist. Guess based on title.
        return Namespace.fromLegacyString(wiki, namespace);
    }

    @NonNull public WikiSite getWikiSite() {
        return wiki;
    }

    @NonNull public String getText() {
        return text.replace(" ", "_");
    }

    @Nullable public String getFragment() {
        return fragment;
    }

    @Nullable public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(@Nullable String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    @Nullable public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NonNull public String getDisplayText() {
        return getPrefixedText().replace("_", " ");
    }

    public boolean hasProperties() {
        return properties != null;
    }

    @Nullable public PageProperties getProperties() {
        return properties;
    }

    public boolean isMainPage() {
        if (properties != null) {
            return properties.isMainPage();
        }
        String mainPageTitle = MainPageNameData.valueFor(getWikiSite().languageCode());
        return mainPageTitle.equals(getDisplayText());
    }

    public boolean isDisambiguationPage() {
        return properties != null && properties.isDisambiguationPage();
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = toIdentifierJSON();
            json.put("languageCode", wiki.languageCode());
            if (properties != null) {
                json.put("properties", properties.toJSON());
            }
            json.put("thumbUrl", getThumbUrl());
            json.put("description", getDescription());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCanonicalUri() {
        return getUriForDomain(getWikiSite().authority());
    }

    public String getMobileUri() {
        return getUriForDomain(getWikiSite().mobileAuthority());
    }

    public String getUriForAction(String action) {
        try {
            return String.format(
                    "%1$s://%2$s/w/index.php?title=%3$s&action=%4$s",
                    getWikiSite().scheme(),
                    getWikiSite().authority(),
                    URLEncoder.encode(getPrefixedText(), "utf-8"),
                    action
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPrefixedText() {

        // TODO: find a better way to check if the namespace is a ISO Alpha2 Code (two digits country code)
        return namespace == null ? getText() : StringUtil.addUnderscores(namespace) + ":" + getText();
    }

    /**
     * Check if the Title represents a File:
     *
     * @return true if it is a File page, false if not
     */
    public boolean isFilePage() {
        return namespace().file();
    }

    /**
     * Check if the Title represents a special page
     *
     * @return true if it is a special page, false if not
     */
    public boolean isSpecial() {
        return namespace().special();
    }

    /**
     * Check if the Title represents a talk page
     *
     * @return true if it is a talk page, false if not
     */
    public boolean isTalkPage() {
        return namespace().talk();
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(text);
        parcel.writeString(fragment);
        parcel.writeParcelable(wiki, flags);
        parcel.writeParcelable(properties, flags);
        parcel.writeString(thumbUrl);
        parcel.writeString(description);
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof PageTitle)) {
            return false;
        }

        PageTitle other = (PageTitle)o;
        // Not using namespace directly since that can be null
        return StringUtil.normalizedEquals(other.getPrefixedText(), getPrefixedText()) && other.wiki.equals(wiki);
    }

    @Override public int hashCode() {
        int result = getPrefixedText().hashCode();
        result = 31 * result + wiki.hashCode();
        return result;
    }

    @Override public String toString() {
        return getPrefixedText();
    }

    @Override public int describeContents() {
        return 0;
    }

    /** Please keep the ID stable. */
    private JSONObject toIdentifierJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("namespace", getNamespace());
            json.put("text", getText());
            json.put("fragment", getFragment());
            json.put("site", wiki.authority());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUriForDomain(String domain) {
        try {
            return String.format(
                    "%1$s://%2$s/wiki/%3$s%4$s",
                    getWikiSite().scheme(),
                    domain,
                    URLEncoder.encode(getPrefixedText(), "utf-8"),
                    (this.fragment != null && this.fragment.length() > 0) ? ("#" + this.fragment) : ""
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private PageTitle(Parcel in) {
        namespace = in.readString();
        text = in.readString();
        fragment = in.readString();
        wiki = in.readParcelable(WikiSite.class.getClassLoader());
        properties = in.readParcelable(PageProperties.class.getClassLoader());
        thumbUrl = in.readString();
        description = in.readString();
    }
}
