package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private final Date lastModified;
    private final String displayTitleText;
    private final String editProtectionStatus;
    private final boolean isMainPage;
    private final String wikiDataId;
    private final String leadImageUrl;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);

    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
    private final boolean canEdit;

    /**
     * Create a new PageProperties object.
     *
     * @param lastModifiedText Last modified date in ISO8601 format
     * @param displayTitleText The title to be displayed for this page
     * @param editProtectionStatus The edit protection status applied to this page
     */
    public PageProperties(String lastModifiedText, String displayTitleText,
                          String editProtectionStatus, boolean canEdit, boolean isMainPage,
                          String wikiDataId, String leadImageUrl) {
        lastModified = new Date();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            lastModified.setTime(sdf.parse(lastModifiedText).getTime());
        } catch (ParseException e) {
            Log.d("PageProperties", "Failed to parse date: " + lastModifiedText);
        }
        this.displayTitleText = displayTitleText;
        this.editProtectionStatus = editProtectionStatus;
        this.canEdit = canEdit;
        this.isMainPage = isMainPage;
        this.wikiDataId = wikiDataId;
        this.leadImageUrl = leadImageUrl;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getDisplayTitle() {
        return displayTitleText;
    }

    public String getEditProtectionStatus() {
        return editProtectionStatus;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean isMainPage() {
        return isMainPage;
    }

    public String getWikiDataId() {
        return wikiDataId;
    }

    public String getLeadImageUrl() {
        return leadImageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(lastModified.getTime());
        parcel.writeString(displayTitleText);
        parcel.writeString(editProtectionStatus);
        parcel.writeInt(canEdit ? 1 : 0);
        parcel.writeInt(isMainPage ? 1 : 0);
        parcel.writeString(wikiDataId);
        parcel.writeString(leadImageUrl);
    }

    private PageProperties(Parcel in) {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        lastModified = new Date(in.readLong());
        displayTitleText = in.readString();
        editProtectionStatus = in.readString();
        canEdit = in.readInt() == 1;
        isMainPage = in.readInt() == 1;
        wikiDataId = in.readString();
        leadImageUrl = in.readString();
    }

    public static final Parcelable.Creator<PageProperties> CREATOR
            = new Parcelable.Creator<PageProperties>() {
        public PageProperties createFromParcel(Parcel in) {
            return new PageProperties(in);
        }

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

        return lastModified.equals(that.lastModified)
                && displayTitleText.equals(that.displayTitleText)
                && canEdit == that.canEdit
                && isMainPage == that.isMainPage
                && TextUtils.equals(editProtectionStatus, that.editProtectionStatus)
                && TextUtils.equals(wikiDataId, that.wikiDataId)
                && TextUtils.equals(leadImageUrl, that.leadImageUrl);
    }

    @Override
    public int hashCode() {
        int result = lastModified.hashCode();
        result = 31 * result + displayTitleText.hashCode();
        if (editProtectionStatus != null) {
            result = 63 * result + editProtectionStatus.hashCode();
        }
        if (leadImageUrl != null) {
            result = 127 * result + leadImageUrl.hashCode();
        }
        if (wikiDataId != null) {
            result = 255 * result + wikiDataId.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("lastmodified", sdf.format(getLastModified()));
            json.put("displaytitle", displayTitleText);
            if (editProtectionStatus == null) {
                json.put("protection", new JSONArray());
            } else {
                JSONObject protectionStatusObject = new JSONObject();
                JSONArray editProtectionStatusArray = new JSONArray();
                editProtectionStatusArray.put(editProtectionStatus);
                protectionStatusObject.put("edit", editProtectionStatusArray);
                json.put("protection", protectionStatusObject);
            }
            json.put("editable", canEdit);
            if (isMainPage) {
                json.put("mainpage", "");
            }
            if (wikiDataId != null) {
                JSONObject pagePropsObject = new JSONObject();
                pagePropsObject.put("wikibase_item", wikiDataId);
                json.put("pageprops", pagePropsObject);
            }
            if (leadImageUrl != null) {
                JSONObject thumbObject = new JSONObject();
                thumbObject.put("url", leadImageUrl);
                json.put("thumb", thumbObject);
            }
        } catch (JSONException e) {
            // Goddamn it Java
            throw new RuntimeException(e);
        }

        return json;
    }

    /**
     * Construct a PageProperties object from JSON returned either from mobileview or toJSON
     *
     * @param json JSON generated either by action=mobileview or by toJSON()
     */
    public static PageProperties parseJSON(JSONObject json) {
        String editProtection = null;
        // Mediawiki API is stupid!
        if (!(json.opt("protection") instanceof JSONArray)
                && json.optJSONObject("protection") != null
                && json.optJSONObject("protection").has("edit")
                ) {
            editProtection = json.optJSONObject("protection").optJSONArray("edit").optString(0);
        }
        String wikiDataId = null;
        String leadImageUrl = null;
        JSONObject pageProps = json.optJSONObject("pageprops");
        if (pageProps != null) {
            wikiDataId = pageProps.optString("wikibase_item");
        }
        JSONObject thumb = json.optJSONObject("thumb");
        if (thumb != null) {
            leadImageUrl = thumb.optString("url");
        }
        return new PageProperties(
                json.optString("lastmodified"),
                json.optString("displaytitle"),
                editProtection,
                json.optBoolean("editable"),
                json.has("mainpage"),
                wikiDataId,
                leadImageUrl
        );
    }
}
