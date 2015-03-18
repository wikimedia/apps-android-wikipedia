package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.staticdata.MainPageNameData;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import static org.wikipedia.util.StringUtil.capitalizeFirstChar;
import static org.wikipedia.util.StringUtil.md5string;
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
    private static final String LANGUAGE_CODE_KEY = "languageCode";

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
    private final String text;
    private final String fragment;
    @Nullable private String thumbUrl;
    private final Site site;
    private String description = null;
    private PageProperties properties = null;

    public PageTitle(@Nullable final String namespace, final String text, final String fragment, @Nullable final String thumbUrl, final Site site) {
        this.namespace = namespace;
        this.text = text;
        this.fragment = fragment;
        this.site = site;
        this.thumbUrl = thumbUrl;
    }

    public PageTitle(final String text, final Site site, @Nullable final String thumbUrl, final String description, final PageProperties properties) {
        this(text, site, thumbUrl);
        this.properties = properties;
        this.description = description;
    }

    public PageTitle(final String text, final Site site, @Nullable final String thumbUrl, final String description) {
        this(text, site, thumbUrl);
        this.description = description;
    }

    public PageTitle(@Nullable final String namespace, final String text, final Site site) {
        this(namespace, text, null, null, site);
    }

    public PageTitle(String text, final Site site, @Nullable String thumbUrl) {
        // FIXME: Does not handle mainspace articles with a colon in the title well at all
        if (TextUtils.isEmpty(text)) {
            // If empty, this refers to the main page.
            text = MainPageNameData.valueFor(site.languageCode());
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
            this.namespace = parts[0];
            this.text = TextUtils.join(":", Arrays.copyOfRange(parts, 1, parts.length));
        } else {
            this.namespace = null;
            this.text = parts[0];
        }

        this.thumbUrl = thumbUrl;
        this.site = site;
    }

    public PageTitle(String text, final Site site) {
        this(text, site, null);
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
        return Namespace.fromLegacyString(site, namespace);
    }

    public Site getSite() {
        return site;
    }

    public String getText() {
        return text.replace(" ", "_");
    }

    public String getFragment() {
        return fragment;
    }

    @Nullable public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(@Nullable String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description != null ? capitalizeFirstChar(description) : null;
    }

    public String getDisplayText() {
        return getPrefixedText().replace("_", " ");
    }

    public boolean hasProperties() {
        return properties != null;
    }

    public PageProperties getProperties() {
        return properties;
    }

    public boolean isMainPage() {
        return hasProperties() && properties.isMainPage();
    }

    public boolean isDisambiguationPage() {
        return hasProperties() && properties.isDisambiguationPage();
    }

    /** Please keep the ID stable. */
    public String getIdentifier() {
        return md5string(toIdentifierJSON().toString());
    }

    /** Please keep the ID stable. */
    private JSONObject toIdentifierJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("namespace", getNamespace());
            json.put("text", getText());
            json.put("fragment", getFragment());
            json.put("site", site.authority());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = toIdentifierJSON();
            json.put(LANGUAGE_CODE_KEY, site.languageCode());
            if (hasProperties()) {
                json.put("properties", getProperties().toJSON());
            }
            json.put("thumbUrl", getThumbUrl());
            json.put("description", getDescription());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public PageTitle(JSONObject json) {
        this.namespace = json.optString("namespace", null);
        this.text = json.optString("text", null);
        this.fragment = json.optString("fragment", null);
        if (json.has("site")) {
            if (json.has(LANGUAGE_CODE_KEY)) {
                site = new Site(json.optString("site"), json.optString(LANGUAGE_CODE_KEY));
            } else {
                // TODO: remove in September 2016.
                site = new Site(json.optString("site"));
            }
        } else {
            site = null;
        }
        this.properties = json.has("properties") ? new PageProperties(json.optJSONObject("properties")) : null;
        this.thumbUrl = json.optString("thumbUrl", null);
        this.description = json.optString("description", null);
    }

    private String getUriForDomain(String domain) {
        try {
            return String.format(
                    "%1$s://%2$s/wiki/%3$s%4$s",
                    getSite().scheme(),
                    domain,
                    URLEncoder.encode(getPrefixedText(), "utf-8"),
                    (this.fragment != null && this.fragment.length() > 0) ? ("#" + this.fragment) : ""
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCanonicalUri() {
        return getUriForDomain(getSite().authority());
    }

    public String getMobileUri() {
        return getUriForDomain(getSite().mobileAuthority());
    }

    public String getUriForAction(String action) {
        try {
            return String.format(
                    "%1$s://%2$s/w/index.php?title=%3$s&action=%4$s",
                    getSite().scheme(),
                    getSite().authority(),
                    URLEncoder.encode(getPrefixedText(), "utf-8"),
                    action
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPrefixedText() {
        return namespace == null ? getText() : namespace + ":" + getText();
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

    @Override
    public int describeContents() {
        return 0;
    }

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

    private PageTitle(Parcel in) {
        namespace = in.readString();
        text = in.readString();
        fragment = in.readString();
        site = in.readParcelable(Site.class.getClassLoader());
        properties = in.readParcelable(PageProperties.class.getClassLoader());
        thumbUrl = in.readString();
        description = in.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(text);
        parcel.writeString(fragment);
        parcel.writeParcelable(site, flags);
        parcel.writeParcelable(properties, flags);
        parcel.writeString(thumbUrl);
        parcel.writeString(description);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageTitle)) {
            return false;
        }

        PageTitle other = (PageTitle)o;
        // Not using namespace directly since that can be null
        return other.getPrefixedText().equals(getPrefixedText()) && other.site.equals(site);
    }

    @Override
    public int hashCode() {
        int result = getPrefixedText().hashCode();
        result = 31 * result + site.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getPrefixedText();
    }
}
