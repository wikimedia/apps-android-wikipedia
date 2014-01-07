package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private final Date lastModified;

    public PageProperties(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getLastModified() {
        return lastModified;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(lastModified.getTime());
    }

    private PageProperties(Parcel in) {
        lastModified = new Date(in.readLong());
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageProperties that = (PageProperties) o;

        if (!lastModified.equals(that.lastModified)) return false;

        return true;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("lastmodifieddate", getLastModified().getTime());
        } catch (JSONException e) {
            // Goddamn it Java
            throw new RuntimeException(e);
        }

        return json;
    }

    public PageProperties(JSONObject json) {
        this(new Date(json.optLong("lastmodifieddate")));
    }
}
