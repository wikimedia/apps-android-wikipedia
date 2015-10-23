package org.wikipedia.page;

import org.wikipedia.data.GsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.annotations.Expose;

import static org.wikipedia.util.StringUtil.compareStrings;

/**
 * Gson POJO for one section of a page.
 */
public class Section {

    @Expose private int id;
    @Expose private int toclevel = 1;
    @Expose private String line;
    @Expose private String anchor;
    @Expose private String text;

    // TODO: can we get rid of this? It's not efficient to
    public static Section fromJson(JSONObject json) {
        return GsonUtil.getDefaultGson().fromJson(json.toString(), Section.class);
    }

    // TODO: get rid of this; problem is how to interop Gson and org.json.JSONObject
    // We're using this to send the section over the JS bridge
    public JSONObject toJSON() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("toclevel", toclevel);
            data.put("line", line);
            data.put("anchor", anchor);
            data.put("text", text);
            return data;
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    /** Default constructor used by Gson deserialization. Good for setting default values. */
    public Section() {
        toclevel = 1;
    }

    public Section(int id, int level, String heading, String anchor, String content) {
        this.id = id;
        this.toclevel = level;
        this.line = heading;
        this.anchor = anchor;
        this.text = content;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Section)) {
            return false;
        }

        Section other = (Section) o;
        return getId() == other.getId()
                && getLevel() == other.getLevel()
                && compareStrings(getHeading(), other.getHeading())
                && compareStrings(getAnchor(), other.getAnchor())
                && compareStrings(getContent(), other.getContent());
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
                + "id=" + id
                + ", toclevel=" + toclevel
                + ", line='" + line + '\''
                + ", anchor='" + anchor + '\''
                + ", text='" + text + '\''
                + '}';
    }

    public boolean isLead() {
        return id == 0;
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return toclevel;
    }

    public String getHeading() {
        return line;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getContent() {
        return text;
    }

    public void setContent(String content) {
        this.text = content;
    }
}
