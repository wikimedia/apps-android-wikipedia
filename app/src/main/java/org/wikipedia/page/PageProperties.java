package org.wikipedia.page;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageLeadProperties;
import org.wikipedia.util.DimenUtil;

import java.text.ParseException;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.util.DateUtil.getIso8601DateFormat;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private static final String JSON_NAME_TITLE_PRONUNCIATION_URL = "titlePronunciationUrl";
    private static final String JSON_NAME_GEO = "geo";
    private static final String JSON_NAME_NAMESPACE = "namespace";

    private final int pageId;
    @NonNull private final Namespace namespace;
    private final long revisionId;
    private final Date lastModified;
    private final String displayTitleText;
    private final String editProtectionStatus;
    private final int languageCount;
    private final boolean isMainPage;
    private final boolean isDisambiguationPage;
    /** Nullable URL with no scheme. For example, foo.bar.com/ instead of http://foo.bar.com/. */
    @Nullable private final String leadImageUrl;
    private final String leadImageName;
    @Nullable private final String titlePronunciationUrl;
    @Nullable private final Location geo;
    @Nullable private final String wikiBaseItem;

    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
    private final boolean canEdit;

    /**
     * Side note: Should later be moved out of this class but I like the similarities with
     * PageProperties(JSONObject).
     */
    public PageProperties(@NonNull WikiSite wiki, PageLeadProperties core) {
        pageId = core.getId();
        namespace = core.getNamespace(wiki);
        revisionId = core.getRevision();
        displayTitleText = defaultString(core.getDisplayTitle());
        titlePronunciationUrl = core.getTitlePronunciationUrl();
        geo = core.getGeo();
        editProtectionStatus = core.getFirstAllowedEditorRole();
        languageCount = core.getLanguageCount();

        // todo: don't hardcode this here
        leadImageUrl = core.getLeadImageUrl(DimenUtil.calculateLeadImageWidth());

        leadImageName = core.getLeadImageFileName();
        lastModified = new Date();
        String lastModifiedText = core.getLastModified();
        if (lastModifiedText != null) {
            try {
                lastModified.setTime(getIso8601DateFormat().parse(lastModifiedText).getTime());
            } catch (ParseException e) {
                Log.d("PageProperties", "Failed to parse date: " + lastModifiedText);
            }
        }
        // assume formatversion=2 is used so we get real booleans from the API
        canEdit = core.isEditable();

        isMainPage = core.isMainPage();
        isDisambiguationPage = core.isDisambiguation();
        wikiBaseItem = core.getWikiBaseItem();
    }

    /**
     * Constructor to be used when building a Page from a compilation. Initializes the title and
     * namespace fields, and explicitly disables editing. All other fields initialized to defaults.
     * @param title Title to which these properties apply.
     */
    public PageProperties(@NonNull PageTitle title, boolean isMainPage) {
        pageId = 0;
        namespace = title.namespace();
        revisionId = 0;
        displayTitleText = title.getDisplayText();
        titlePronunciationUrl = null;
        geo = null;
        editProtectionStatus = "";
        languageCount = 1;
        leadImageUrl = null;
        leadImageName = "";
        lastModified = new Date();
        canEdit = false;
        this.isMainPage = isMainPage;
        isDisambiguationPage = false;
        wikiBaseItem = null;
    }

    /**
     * Create a new PageProperties object.
     * @param json JSON object from which this item will be built.
     */
    public PageProperties(JSONObject json) {
        pageId = json.optInt("id");
        namespace = Namespace.of(json.optInt(JSON_NAME_NAMESPACE));
        revisionId = json.optLong("revision");
        displayTitleText = json.optString("displaytitle");
        titlePronunciationUrl = json.optString(JSON_NAME_TITLE_PRONUNCIATION_URL, null);
        geo = GeoUnmarshaller.unmarshal(json.optString(JSON_NAME_GEO, null));

        if (json.optJSONObject("protection") != null && json.optJSONObject("protection").has("edit")) {
            editProtectionStatus = json.optJSONObject("protection").optJSONArray("edit").optString(0);
        } else {
            editProtectionStatus = null;
        }
        languageCount = json.optInt("languagecount");
        JSONObject thumb = json.optJSONObject("thumb");
        leadImageUrl = thumb != null ? thumb.optString("url") : null;
        JSONObject image = json.optJSONObject("image");
        leadImageName = image != null ? image.optString("file") : null;
        lastModified = new Date();
        String lastModifiedText = json.optString("lastmodified");
        try {
            lastModified.setTime(getIso8601DateFormat().parse(lastModifiedText).getTime());
        } catch (ParseException e) {
            Log.d("PageProperties", "Failed to parse date: " + lastModifiedText);
        }
        // There's something really screwy going on with the "editable" key in the API response.
        // It's not always returning a boolean, sadly.
        // If the key is the empty string, or true, then the page is editable.
        // If the key is not in the response, or is false, then the page is not editable.
        // This solution, while stupid, will work even if the API starts returning a boolean.
        canEdit = (json.has("editable") && json.optString("editable").equals(""))
                || json.optString("editable").equals("true");

        isMainPage = json.has("mainpage");
        isDisambiguationPage = json.has("disambiguation");
        wikiBaseItem = json.optString("wikibase_item");
    }

    public int getPageId() {
        return pageId;
    }

    @NonNull public Namespace getNamespace() {
        return namespace;
    }

    public long getRevisionId() {
        return revisionId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getDisplayTitle() {
        return displayTitleText;
    }

    @Nullable
    public String getTitlePronunciationUrl() {
        return titlePronunciationUrl;
    }

    @Nullable
    public Location getGeo() {
        return geo;
    }

    public String getEditProtectionStatus() {
        return editProtectionStatus;
    }

    public int getLanguageCount() {
        return languageCount;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean isMainPage() {
        return isMainPage;
    }

    public boolean isDisambiguationPage() {
        return isDisambiguationPage;
    }

    /**
     * @return Nullable URL with no scheme. For example, foo.bar.com/ instead of
     *         http://foo.bar.com/.
     */
    @Nullable
    public String getLeadImageUrl() {
        return leadImageUrl;
    }

    public String getLeadImageName() {
        return leadImageName;
    }

    @Nullable
    public String getWikiBaseItem() {
        return wikiBaseItem;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(pageId);
        parcel.writeInt(namespace.code());
        parcel.writeLong(revisionId);
        parcel.writeLong(lastModified.getTime());
        parcel.writeString(displayTitleText);
        parcel.writeString(titlePronunciationUrl);
        parcel.writeString(GeoMarshaller.marshal(geo));
        parcel.writeString(editProtectionStatus);
        parcel.writeInt(languageCount);
        parcel.writeInt(canEdit ? 1 : 0);
        parcel.writeInt(isMainPage ? 1 : 0);
        parcel.writeInt(isDisambiguationPage ? 1 : 0);
        parcel.writeString(leadImageUrl);
        parcel.writeString(leadImageName);
        parcel.writeString(wikiBaseItem);
    }

    private PageProperties(Parcel in) {
        pageId = in.readInt();
        namespace = Namespace.of(in.readInt());
        revisionId = in.readLong();
        lastModified = new Date(in.readLong());
        displayTitleText = in.readString();
        titlePronunciationUrl = in.readString();
        geo = GeoUnmarshaller.unmarshal(in.readString());
        editProtectionStatus = in.readString();
        languageCount = in.readInt();
        canEdit = in.readInt() == 1;
        isMainPage = in.readInt() == 1;
        isDisambiguationPage = in.readInt() == 1;
        leadImageUrl = in.readString();
        leadImageName = in.readString();
        wikiBaseItem = in.readString();
    }

    public static final Parcelable.Creator<PageProperties> CREATOR
            = new Parcelable.Creator<PageProperties>() {
        @Override
        public PageProperties createFromParcel(Parcel in) {
            return new PageProperties(in);
        }

        @Override
        public PageProperties[] newArray(int size) {
            return new PageProperties[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PageProperties that = (PageProperties) o;

        return pageId == that.pageId
                && namespace == that.namespace
                && revisionId == that.revisionId
                && lastModified.equals(that.lastModified)
                && displayTitleText.equals(that.displayTitleText)
                && TextUtils.equals(titlePronunciationUrl, that.titlePronunciationUrl)
                && (geo == that.geo || geo != null && geo.equals(that.geo))
                && languageCount == that.languageCount
                && canEdit == that.canEdit
                && isMainPage == that.isMainPage
                && isDisambiguationPage == that.isDisambiguationPage
                && TextUtils.equals(editProtectionStatus, that.editProtectionStatus)
                && TextUtils.equals(leadImageUrl, that.leadImageUrl)
                && TextUtils.equals(leadImageName, that.leadImageName)
                && TextUtils.equals(wikiBaseItem, that.wikiBaseItem);
    }

    @Override
    public int hashCode() {
        int result = lastModified.hashCode();
        result = 31 * result + displayTitleText.hashCode();
        result = 31 * result + (titlePronunciationUrl != null ? titlePronunciationUrl.hashCode() : 0);
        result = 31 * result + (geo != null ? geo.hashCode() : 0);
        result = 31 * result + (editProtectionStatus != null ? editProtectionStatus.hashCode() : 0);
        result = 31 * result + languageCount;
        result = 31 * result + (isMainPage ? 1 : 0);
        result = 31 * result + (isDisambiguationPage ? 1 : 0);
        result = 31 * result + (leadImageUrl != null ? leadImageUrl.hashCode() : 0);
        result = 31 * result + (leadImageName != null ? leadImageName.hashCode() : 0);
        result = 31 * result + (wikiBaseItem != null ? wikiBaseItem.hashCode() : 0);
        result = 31 * result + (canEdit ? 1 : 0);
        result = 31 * result + pageId;
        result = 31 * result + namespace.code();
        result = 31 * result + (int) revisionId;
        return result;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", pageId);
            json.put(JSON_NAME_NAMESPACE, namespace.code());
            json.put("revision", revisionId);
            json.put("lastmodified", getIso8601DateFormat().format(getLastModified()));
            json.put("displaytitle", displayTitleText);
            json.put(JSON_NAME_TITLE_PRONUNCIATION_URL, titlePronunciationUrl);
            json.put(JSON_NAME_GEO, GeoMarshaller.marshal(geo));
            json.put("wikibase_item", wikiBaseItem);
            if (editProtectionStatus == null) {
                json.put("protection", new JSONArray());
            } else {
                JSONObject protectionStatusObject = new JSONObject();
                JSONArray editProtectionStatusArray = new JSONArray();
                editProtectionStatusArray.put(editProtectionStatus);
                protectionStatusObject.put("edit", editProtectionStatusArray);
                json.put("protection", protectionStatusObject);
            }
            json.put("languagecount", languageCount);
            json.put("editable", canEdit);
            if (isMainPage) {
                json.put("mainpage", "");
            }
            if (isDisambiguationPage) {
                json.put("disambiguation", "");
            }
            if (leadImageUrl != null) {
                JSONObject thumbObject = new JSONObject();
                thumbObject.put("url", leadImageUrl);
                json.put("thumb", thumbObject);
            }
            if (leadImageName != null) {
                JSONObject imageObject = new JSONObject();
                imageObject.put("file", leadImageName);
                json.put("image", imageObject);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return json;
    }
}
