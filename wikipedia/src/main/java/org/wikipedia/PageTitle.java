package org.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.staticdata.SpecialAliasData;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Immutable value object representing the text of a page.
 *
 * Points to a specific page in a specific namespace on a specific site.
 * Is immutable.
 */
public class PageTitle implements Parcelable {
    private final String namespace;
    private final String text;
    private final String fragment;
    private final Site site;

    public PageTitle(final String namespace, final String text, final String fragment, final Site site) {
        this.namespace = namespace;
        this.text = text;
        this.fragment = fragment;
        this.site = site;
    }

    public PageTitle(final String namespace, final String text, final Site site) {
        this(namespace, text, null, site);
    }

    public PageTitle(String text, final Site site) {
        // FIXME: Does not handle mainspace articles with a colon in the title well at all
        if (text.equals("")) {
            // If empty, this refers to the main page.
            text = MainPageNameData.valueFor(site.getLanguage());
        }

        String[] fragParts = text.split("#", -1);
        text = fragParts[0];
        if (fragParts.length > 1) {
            try {
                this.fragment = URLDecoder.decode(fragParts[1], "utf-8");
            } catch (UnsupportedEncodingException e) {
                // STUPID STUPID JAVA
                throw new RuntimeException(e);
            }
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

        this.site = site;
    }

    public String getNamespace() {
        return namespace;
    }

    public Site getSite() {
        return site;
    }

    public String getText() {
        return text;
    }

    public String getFragment() { return fragment; }

    public String getDisplayText() {
        return getPrefixedText().replace("_", " ");
    }

    public String getHashedItentifier() {
        return Utils.md5(String.format("%1$s/%2$s", getSite().getDomain(), getPrefixedText()));
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("site", site.getDomain());
            json.put("namespace", getNamespace());
            json.put("text", getText());
            json.put("fragment", getFragment());
            return json;
        } catch (JSONException e) {
            // This will also never happen
            throw new RuntimeException(e);
        }
    }

    public PageTitle(JSONObject json) {
        this.site = new Site(json.optString("site"));
        this.namespace = json.optString("namespace", null);
        this.fragment = json.optString("fragment", null);
        this.text = json.optString("text", null);
    }

    private String getUriForDomain(String domain) {
        try {
            return String.format(
                    "%1$s://%2$s/wiki/%3$s%4$s",
                    WikipediaApp.PROTOCOL,
                    domain,
                    URLEncoder.encode(getPrefixedText().replace(" ", "_"), "utf-8"),
                    (this.fragment != null && this.fragment.length() > 0) ? ("#" + this.fragment) : ""
            );
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public String getCanonicalUri() {
        return getUriForDomain(getSite().getDomain());
    }

    public String getMobileUri() {
        return getUriForDomain(getSite().getApiDomain());
    }

    public String getUriForAction(String action) {
        try {
            return String.format(
                    "%1$s://%2$s/w/index.php?title=%3$s&action=%4$s",
                    WikipediaApp.PROTOCOL,
                    getSite().getDomain(),
                    URLEncoder.encode(getPrefixedText().replace(" ", "_"), "utf-8"),
                    action
            );
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public String getPrefixedText() {
        return namespace == null ? text : namespace + ":" + text;
    }

    private Boolean isSpecial = null;
    public boolean isSpecial() {
        if (isSpecial == null) {
            isSpecial = getNamespace() != null
                    && SpecialAliasData.valueFor(getSite().getLanguage()).equals(getNamespace());
        }

        return isSpecial;
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
        fragment = in.readString();
        site = in.readParcelable(Site.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(text);
        parcel.writeString(fragment);
        parcel.writeParcelable(site, flags);
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
