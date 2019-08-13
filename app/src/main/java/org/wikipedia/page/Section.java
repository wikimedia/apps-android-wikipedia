package org.wikipedia.page;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.json.GsonUtil;

import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * Gson POJO for one section of a page.
 */
public class Section {

    private int id;
    private int toclevel = 1;
    private String line;
    private String anchor;
    private String text;

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

    @NonNull public String getHeading() {
        return defaultString(line);
    }

    @NonNull public String getAnchor() {
        return defaultString(anchor);
    }

    @NonNull public String getContent() {
        return defaultString(text);
    }
}
