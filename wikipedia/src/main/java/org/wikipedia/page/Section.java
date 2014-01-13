package org.wikipedia.page;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a particular section of an article.
 */
public class Section implements Parcelable {

    private final JSONObject data;

    private ArrayList<Section> subSections = new ArrayList<Section>();

    public Section(JSONObject json) {
        this.data = json;
        if (data.has("subSections")) {
            JSONArray subsectionsJSON = data.optJSONArray("subSections");
            for (int i = 0; i < subsectionsJSON.length(); i++) {
                subSections.add(new Section(subsectionsJSON.optJSONObject(i)));
            }
        }
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
                && Utils.compareStrings(getContent(), other.getContent())
                && getSubSections().equals(other.getSubSections());

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

    public void insertSection(Section subSection) {
        subSections.add(subSection);
    }

    public ArrayList<Section> getSubSections() {
        return subSections;
    }

    public JSONObject toJSON() {
        // Always regenerate the subsections, since that's mutable.
        JSONArray subSectionsJSON = new JSONArray();
        for (Section s : getSubSections()) {
            subSectionsJSON.put(s.toJSON());
        }

        try {
            data.put("subSections", subSectionsJSON);
        } catch (JSONException e) {
            // Won't happen
            throw new RuntimeException(e);
        }
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
        // We could use binary search here, but meh. Fast enough.
        for (int i = 0; i < sections.size(); i++) {
            Section curSection = sections.get(i);
            if (curSection.getId() == id) {
                return curSection;
            }
            if (i + 1 < sections.size()) {
                Section nextSection = sections.get(i + 1);
                if (id < nextSection.getId() && id > curSection.getId()) {
                    return findSectionForID(curSection.getSubSections(), id);
                }
            } else {
                return findSectionForID(curSection.getSubSections(), id);
            }
        }
        throw new RuntimeException("We can't find that section with id " + id + " AND THIS IS NOT SUPPOSED TO HAPPEN");
    }
}
