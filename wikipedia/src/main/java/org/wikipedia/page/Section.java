package org.wikipedia.page;

import android.os.*;
import org.json.*;
import org.wikipedia.*;

import java.util.*;

/**
 * Represents a particular section of an article.
 */
public class Section implements Parcelable {

    private final JSONObject data;

    public Section(JSONObject json) {
        this.data = json;
    }

    // Use this only for tests and such.
    public Section(int id, int level, String heading, String anchor, String content) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("toclevel", level);
            obj.put("line", heading);
            obj.put("anchor", anchor);
            obj.put("text", content);
            data = obj;
        } catch (JSONException e) {
            // This, also, will never happen. Very similar to Java being sane, some say.
            throw new RuntimeException(e);
        }
    }

    // This won't actually throw
    private Section(Parcel in) throws JSONException {
        this(new JSONObject(in.readString()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Section)) {
            return false;
        }

        Section other = (Section) o;
        return getId() == other.getId()
                && getLevel() == other.getLevel()
                && Utils.compareStrings(getHeading(), other.getHeading())
                && Utils.compareStrings(getAnchor(), other.getAnchor())
                && Utils.compareStrings(getContent(), other.getContent());

    }

    public boolean isLead() {
        return getId() == 0;
    }

    public int getId() {
        return data.optInt("id");
    }

    public int getLevel() {
        return data.optInt("toclevel", 1);
    }

    public String getHeading() {
        return data.optString("line");
    }

    public String getAnchor() {
        return data.optString("anchor");
    }

    public String getContent() {
        return data.optString("text");
    }

    public JSONObject toJSON() {
        return data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(toJSON().toString());
    }

    public static final Parcelable.Creator<Section> CREATOR
            = new Parcelable.Creator<Section>() {
        public Section createFromParcel(Parcel in) {
            try {
                return new Section(in);
            } catch (JSONException e) {
                // This won't happen
                throw new RuntimeException(e);
            }
        }

        public Section[] newArray(int size) {
            return new Section[size];
        }
    };

    public static Section findSectionForID(List<Section> sections, int id) {
        return sections.get(id);
    }
}
