package org.wikimedia.wikipedia;

import java.util.ArrayList;

/**
 * Represents a particular section of an article.
 */
public class Section {
    private int id;
    private int level;
    private String heading;
    private String anchor;
    private String content;

    private ArrayList<Section> subSections = new ArrayList<Section>();

    public Section(int id, int level, String heading, String anchor, String content) {
        this.id = id;
        this.level = level;
        this.heading = heading;
        this.anchor = anchor;
        this.content = content;
    }

    public boolean isLead() {
        return level == 0;
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public String getHeading() {
        return heading;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getContent() {
        return content;
    }

    public void insertSection(Section subSection) {
        subSections.add(subSection);
    }

    public ArrayList<Section> getSubSections() {
        return subSections;
    }

    public String toHTML() {
        String html = "";

        if (!isLead()) {
            int headingLevel = getLevel() + 1;
            html = "<h" + headingLevel + ">" + getHeading() + "</h" + headingLevel + ">";
        }

        html += "<div>";
        html += getContent();
        for (Section s : subSections) {
            html += s.toHTML();
        }
        html += "</div>";

        return html;
    }
}
