package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.language.AppLanguageLookUpTable;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.staticdata.TalkAliasData;
import org.wikipedia.staticdata.UserTalkAliasData;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;

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
    @NonNull private String text;
    @Nullable private String fragment;
    @Nullable private String thumbUrl;
    @SerializedName("site") @NonNull private final WikiSite wiki;
    @Nullable private String description;
    // TODO: remove after the restbase endpoint supports ZH variants.
    @Nullable private String displayText;
    @Nullable private String extract;

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
            return new PageTitle(prefixedText, wiki, null);
        } else {
            // TODO: this class needs some refactoring to allow passing in a fragment
            // without having to do string manipulations.
            return new PageTitle(prefixedText + "#" + fragment, wiki, null);
        }
    }

    public PageTitle(@Nullable final String namespace, @NonNull String text, @Nullable String fragment, @Nullable String thumbUrl, @NonNull WikiSite wiki) {
        this.namespace = namespace;
        this.text = text;
        this.fragment = fragment;
        this.wiki = wiki;
        this.thumbUrl = thumbUrl;
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl, @Nullable String description, @Nullable String displayText) {
        this(text, wiki, thumbUrl);
        this.displayText = displayText;
        this.description = description;
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl, @Nullable String description, @Nullable String displayText, @Nullable String extract) {
        this(text, wiki, thumbUrl, description, displayText);
        this.extract = extract;
    }

    public PageTitle(@Nullable String namespace, @NonNull String text, @NonNull WikiSite wiki) {
        this(namespace, text, null, null, wiki);
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki) {
        this(text, wiki, null);
    }

    public PageTitle(@Nullable String text, @NonNull WikiSite wiki, @Nullable String thumbUrl) {
        // FIXME: Does not handle mainspace articles with a colon in the title well at all
        if (TextUtils.isEmpty(text)) {
            // If empty, this refers to the main page.
            text = SiteInfoClient.getMainPageForLang(wiki.languageCode());
        }

        // Remove any URL parameters (?...) from the title
        String[] parts = text.split("\\?", -1);
        if (parts.length > 1 && parts[1].contains("=")) {
            text = parts[0];
        }

        // Split off any fragment (#...) from the title
        parts = text.split("#", -1);
        text = parts[0];
        if (parts.length > 1) {
            this.fragment = StringUtil.addUnderscores(decodeURL(parts[1]));
        } else {
            this.fragment = null;
        }

        parts = text.split(":", -1);
        if (parts.length > 1) {
            String namespaceOrLanguage = parts[0];
            if (Arrays.asList(Locale.getISOLanguages()).contains(namespaceOrLanguage)) {
                this.namespace = null;
                this.wiki = new WikiSite(wiki.authority(), namespaceOrLanguage);
                this.text = TextUtils.join(":", Arrays.copyOfRange(parts, 1, parts.length));
            } else if (parts[1].length() > 0 && !Character.isWhitespace(parts[1].charAt(0)) && (parts[1].charAt(0) != '_')) {
                this.wiki = wiki;
                this.namespace = namespaceOrLanguage;
                this.text = TextUtils.join(":", Arrays.copyOfRange(parts, 1, parts.length));
            } else {
                this.wiki = wiki;
                this.namespace = null;
                this.text = text;
            }
        } else {
            this.wiki = wiki;
            this.namespace = null;
            this.text = text;
        }

        this.thumbUrl = thumbUrl;
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @NonNull public Namespace namespace() {
        return Namespace.fromLegacyString(wiki, StringUtil.removeUnderscores(namespace));
    }

    @NonNull public WikiSite getWikiSite() {
        return wiki;
    }

    @NonNull public String getText() {
        return StringUtil.addUnderscores(text);
    }

    @Nullable public String getFragment() {
        return fragment;
    }

    public void setFragment(@Nullable String fragment) {
        this.fragment = fragment;
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

    @NonNull
    public String getExtract() {
        return StringUtils.defaultString(extract);
    }

    // This update the text to the API text.
    public void setText(@NonNull String convertedFromText) {
        this.text = convertedFromText;
    }

    @NonNull public String getDisplayText() {
        return displayText == null ? StringUtil.removeUnderscores(getPrefixedText()) : displayText;
    }

    public void setDisplayText(@Nullable String displayText) {
        this.displayText = displayText;
    }

    public boolean isMainPage() {
        String mainPageTitle = SiteInfoClient.getMainPageForLang(getWikiSite().languageCode());
        return mainPageTitle.equals(getDisplayText());
    }

    public String getUri() {
        return getUriForDomain(getWikiSite().authority());
    }

    public String getMobileUri() {
        return getUriForDomain(getWikiSite().authority().replace(".wikipedia.org", ".m.wikipedia.org"));
    }

    public String getWebApiUrl(String fragment) {
        return String.format(
                "%1$s://%2$s/w/index.php?title=%3$s&%4$s",
                getWikiSite().scheme(),
                getWikiSite().authority(),
                UriUtil.encodeURL(getPrefixedText()),
                fragment
        );
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

    public PageTitle pageTitleForTalkPage() {
        String talkNamespace = namespace().user() || namespace().userTalk()
                ? UserTalkAliasData.valueFor(wiki.languageCode()) : TalkAliasData.valueFor(wiki.languageCode());
        PageTitle pageTitle = new PageTitle(talkNamespace, getText(), wiki);
        pageTitle.setDisplayText(talkNamespace + ":" + (!TextUtils.isEmpty(getNamespace()) && getDisplayText().startsWith(getNamespace())
                ? StringUtil.removeNamespace(getDisplayText()) : getDisplayText()));
        return pageTitle;
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(text);
        parcel.writeString(fragment);
        parcel.writeParcelable(wiki, flags);
        parcel.writeString(thumbUrl);
        parcel.writeString(description);
        parcel.writeString(displayText);
        parcel.writeString(extract);
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

    private String getUriForDomain(String domain) {
        return String.format(
                "%1$s://%2$s/%3$s/%4$s%5$s",
                getWikiSite().scheme(),
                domain,
                domain.startsWith(AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE) ? getWikiSite().languageCode() : "wiki",
                UriUtil.encodeURL(getPrefixedText()),
                (this.fragment != null && this.fragment.length() > 0) ? ("#" + UriUtil.encodeURL(this.fragment)) : ""
        );
    }

    private PageTitle(Parcel in) {
        namespace = in.readString();
        text = in.readString();
        fragment = in.readString();
        wiki = in.readParcelable(WikiSite.class.getClassLoader());
        thumbUrl = in.readString();
        description = in.readString();
        displayText = in.readString();
        extract = in.readString();
    }
}
