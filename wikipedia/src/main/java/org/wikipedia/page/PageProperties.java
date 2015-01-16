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
    private final int pageId;
    private final long revisionId;
    private final Date lastModified;
    private final String displayTitleText;
    private final String editProtectionStatus;
    private final boolean isMainPage;
    private final String leadImageUrl;
    private final String leadImageName;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);

    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
    private final boolean canEdit;

    /**
     * Create a new PageProperties object.
     * @param json JSON object from which this item will be built.
     */
    public PageProperties(JSONObject json) {
        pageId = json.optInt("id");
        revisionId = json.optLong("revision");
        displayTitleText = json.optString("displaytitle");
        // Mediawiki API is stupid!
        if (!(json.opt("protection") instanceof JSONArray)
            && json.optJSONObject("protection") != null
            && json.optJSONObject("protection").has("edit")
                ) {
            editProtectionStatus = json.optJSONObject("protection").optJSONArray("edit").optString(0);
        } else {
            editProtectionStatus = null;
        }
        JSONObject thumb = json.optJSONObject("thumb");
        leadImageUrl = thumb != null ? thumb.optString("url") : null;
        JSONObject image = json.optJSONObject("image");
        leadImageName = image != null ? image.optString("file") : null;
        lastModified = new Date();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String lastModifiedText = json.optString("lastmodified");
        try {
            lastModified.setTime(sdf.parse(lastModifiedText).getTime());
        } catch (ParseException e) {
            Log.d("PageProperties", "Failed to parse date: " + lastModifiedText);
        }
        canEdit = json.optBoolean("editable");
        isMainPage = json.has("mainpage");
    }

    public int getPageId() {
        return pageId;
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

    public String getEditProtectionStatus() {
        return editProtectionStatus;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean isMainPage() {
        return isMainPage;
    }

    public String getLeadImageUrl() {
        return leadImageUrl;
    }

    public String getLeadImageName() {
        return leadImageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(pageId);
        parcel.writeLong(revisionId);
        parcel.writeLong(lastModified.getTime());
        parcel.writeString(displayTitleText);
        parcel.writeString(editProtectionStatus);
        parcel.writeInt(canEdit ? 1 : 0);
        parcel.writeInt(isMainPage ? 1 : 0);
        parcel.writeString(leadImageUrl);
        parcel.writeString(leadImageName);
    }

    private PageProperties(Parcel in) {
        pageId = in.readInt();
        revisionId = in.readLong();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        lastModified = new Date(in.readLong());
        displayTitleText = in.readString();
        editProtectionStatus = in.readString();
        canEdit = in.readInt() == 1;
        isMainPage = in.readInt() == 1;
        leadImageUrl = in.readString();
        leadImageName = in.readString();
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

        return pageId == that.pageId
                && revisionId == that.revisionId
                && lastModified.equals(that.lastModified)
                && displayTitleText.equals(that.displayTitleText)
                && canEdit == that.canEdit
                && isMainPage == that.isMainPage
                && TextUtils.equals(editProtectionStatus, that.editProtectionStatus)
                && TextUtils.equals(leadImageUrl, that.leadImageUrl)
                && TextUtils.equals(leadImageName, that.leadImageName);
    }

    @Override
    public int hashCode() {
        int result = lastModified.hashCode();
        result = 31 * result + displayTitleText.hashCode();
        result = 31 * result + (editProtectionStatus != null ? editProtectionStatus.hashCode() : 0);
        result = 31 * result + (isMainPage ? 1 : 0);
        result = 31 * result + (leadImageUrl != null ? leadImageUrl.hashCode() : 0);
        result = 31 * result + (leadImageName != null ? leadImageName.hashCode() : 0);
        result = 31 * result + (canEdit ? 1 : 0);
        result = 31 * result + pageId;
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
            json.put("revision", revisionId);
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
            if (leadImageUrl != null) {
                JSONObject thumbObject = new JSONObject();
                thumbObject.put("url", leadImageUrl);
                json.put("thumb", thumbObject);
            }
            if (leadImageName != null) {
                JSONObject imageObject = new JSONObject();
                imageObject.put("file", leadImageUrl);
                json.put("image", imageObject);
            }
        } catch (JSONException e) {
            // Goddamn it Java
            throw new RuntimeException(e);
        }

        return json;
    }
}
