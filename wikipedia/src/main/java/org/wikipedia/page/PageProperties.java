package org.wikipedia.page;

import android.os.*;
import org.json.*;

import java.util.*;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private final Date lastModified;
    private final String displayTitleText;


    public PageProperties(Date lastModified, String displayTitleText) {
        this.lastModified = lastModified;
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
            json.put("lastmodifieddate", getLastModified().getTime());
            json.put("displaytitle", displayTitleText);
        } catch (JSONException e) {
            // Goddamn it Java
            throw new RuntimeException(e);
        }

        return json;
    }

    public PageProperties(JSONObject json) {
        this(
                new Date(json.optLong("lastmodifieddate")),
                json.optString("displaytitle")
        );
    }
}
