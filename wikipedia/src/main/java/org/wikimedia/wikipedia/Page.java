package org.wikimedia.wikipedia;

import java.util.ArrayList;

/**
 * Represents a particular page along with its full contents.
 */
public class Page {
    private final PageTitle title;
    private final ArrayList<Section> sections;

    public Page(PageTitle title, ArrayList<Section> sections) {
        this.title = title;
        this.sections = sections;
    }

    public PageTitle getTitle() {
        return title;
    }

    public ArrayList<Section> getSections() {
        return sections;
    }

    public String getHTML() {
        String html = "";
        for (Section s : sections) {
            html += s.toHTML();
        }
        return html;
    }

}
