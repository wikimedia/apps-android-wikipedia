package org.wikipedia.page;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.json.GsonUtil;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * Gson POJO for one section of a page.
 */
public class Section {

    private int id;
    private int level = 1;
    private String anchor;
    private String text;
    private String title;

    public static List<Section> fromJson(String json) {
        return Arrays.asList(GsonUtil.getDefaultGson().fromJson(json, Section[].class));
    }

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
            data.put("level", level);
            data.put("anchor", anchor);
            data.put("text", text);
            return data;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /** Default constructor used by Gson deserialization. Good for setting default values. */
    public Section() {
        level = 1;
    }

    public Section(int id, int level, String heading, String anchor, String content) {
        this.id = id;
        this.level = level;
        this.title = heading;
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
                && StringUtils.equals(getHeading(), other.getHeading())
                && StringUtils.equals(getAnchor(), other.getAnchor())
                && StringUtils.equals(getContent(), other.getContent());
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
                + ", level=" + level
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
        return level;
    }

    @NonNull public String getHeading() {
        return defaultString(title);
    }

    @NonNull public String getAnchor() {
        return defaultString(anchor);
    }

    @NonNull public String getContent() {
        return defaultString(text);
    }
}
