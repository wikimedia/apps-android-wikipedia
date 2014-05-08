package org.wikipedia.page;

import android.os.*;
import android.util.*;
import org.json.*;

import java.text.*;
import java.util.*;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private final Date lastModified;
    private final String displayTitleText;
    private SimpleDateFormat sdf;

    public PageProperties(String lastModifiedText, String displayTitleText) {
        lastModified = new Date();
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            lastModified.setTime(sdf.parse(lastModifiedText).getTime());
        } catch (ParseException e) {
            Log.d("PageProperties", "Failed to parse date: " + lastModifiedText);
        }
        this.displayTitleText = displayTitleText;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getDisplayTitle() {
        return displayTitleText;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(lastModified.getTime());
        parcel.writeString(displayTitleText);
    }

    private PageProperties(Parcel in) {
        lastModified = new Date(in.readLong());
        displayTitleText = in.readString();
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
                && displayTitleText.equals(that.displayTitleText);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("lastmodified", sdf.format(getLastModified()));
            json.put("displaytitle", displayTitleText);
        } catch (JSONException e) {
            // Goddamn it Java
            throw new RuntimeException(e);
        }

        return json;
    }

    public PageProperties(JSONObject json) {
        this(
                json.optString("lastmodified"),
                json.optString("displaytitle")
        );
    }
}
