package org.wikipedia.page;

import org.wikipedia.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a particular section of an article.
 */
public class Section {

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

    @Override
    public int hashCode() {
        int result = getId();
        result = 31 * result + getHeading().hashCode();
        result = 31 * result + getAnchor().hashCode();
        result = 31 * result + getContent().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Section{"
                + "data=" + data
                + '}';
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

}
