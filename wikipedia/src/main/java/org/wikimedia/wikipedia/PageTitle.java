package org.wikimedia.wikipedia;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Immutable value object representing the text of a page.
 *
 * Points to a specific page in a specific namespace on a specific site.
 * Is immutable.
 */
public class PageTitle implements Parcelable {
    private final String namespace;
    private final String text;
    private final Site site;

    public PageTitle(final String namespace, final String text, final Site site) {
        this.namespace = namespace;
        this.text = text;
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

    public String getDisplayText() {
        return getText().replace("_", " ");
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
            return json;
        } catch (JSONException e) {
            // This will also never happen
            throw new RuntimeException(e);
        }
    }

    public PageTitle(JSONObject json) {
        this.site = new Site(json.optString("site"));
        this.namespace = json.optString("namespace", null);
        this.text = json.optString("text", null);
    }

    public String getCanonicalUri() {
        try {
            return String.format(
                    "%1$s://%2$s/wiki/%3$s",
                    WikipediaApp.PROTOCOL,
                    getSite().getDomain(),
                    URLEncoder.encode(getPrefixedText().replace(" ", "_"), "utf-8")
            );
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
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
        site = in.readParcelable(Site.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(namespace);
        parcel.writeString(text);
        parcel.writeParcelable(site, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageTitle)) {
            return false;
        }

        PageTitle other = (PageTitle)o;
        // Not using namespace directly since that can be null
        return other.getPrefixedText().equals(getPrefixedText()) && other.getSite().equals(getSite());
    }

    @Override
    public String toString() {
        return getPrefixedText();
    }
}
