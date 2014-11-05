package org.wikipedia.page;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.PageTitle;

import java.util.ArrayList;

/**
 * Represents a particular page along with its full contents.
 */
public class Page {
    private final PageTitle title;
    private final ArrayList<Section> sections;
    private final PageProperties pageProperties;

    /**
     * The props to send to api=mobileview to get all the data required for filling up Page object
     */
    public static final String API_REQUEST_PROPS = "lastmodified|normalizedtitle|displaytitle|protection|editable";

    public Page(PageTitle title, ArrayList<Section> sections, PageProperties pageProperties) {
        this.title = title;
        this.sections = sections;
        this.pageProperties = pageProperties;
    }

    public PageTitle getTitle() {
        return title;
    }

    public ArrayList<Section> getSections() {
        return sections;
    }

    public String getDisplayTitle() {
        return pageProperties.getDisplayTitle();
    }

    public PageProperties getPageProperties() {
        return pageProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Page)) {
            return false;
        }

        Page other = (Page) o;
        return title.equals(other.title)
                && sections.equals(other.sections)
                && pageProperties.equals(other.pageProperties);
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + sections.hashCode();
        result = 31 * result + pageProperties.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Page{"
                + "title=" + title
                + ", sections=" + sections
                + ", pageProperties=" + pageProperties
                + '}';
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.putOpt("title", getTitle().toJSON());
            JSONArray sectionsJSON = new JSONArray();
            for (Section section : getSections()) {
                sectionsJSON.put(section.toJSON());
            }
            json.putOpt("sections", sectionsJSON);
            json.putOpt("properties", pageProperties.toJSON());
            return json;
        } catch (JSONException e) {
            // This will never happen. Java stinks.
            throw new RuntimeException(e);
        }
    }

    public Page(JSONObject json) {
        title = new PageTitle(json.optJSONObject("title"));
        JSONArray sectionsJSON = json.optJSONArray("sections");
        sections = new ArrayList<Section>(sectionsJSON.length());
        for (int i = 0; i < sectionsJSON.length(); i++) {
            sections.add(new Section(sectionsJSON.optJSONObject(i)));
        }
        pageProperties = PageProperties.parseJSON(json.optJSONObject("properties"));
    }
}
