package org.wikimedia.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a particular section of an article.
 */
public class Section implements Parcelable {
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

    public Section(Parcel in) {
        id = in.readInt();
        level = in.readInt();
        heading = in.readString();
        anchor = in.readString();
        content = in.readString();

        subSections = in.readArrayList(Section.class.getClassLoader());
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
        return id == 0;
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
        StringBuilder builder = new StringBuilder();

        if (!isLead()) {
            int headingLevel = getLevel() + 1;
            builder.append("<h").append(headingLevel).append(">")
                    .append(getHeading())
                    .append("</h").append(headingLevel).append(">");
        }

        builder.append("<div>")
                .append(getContent());
        for (Section s : subSections) {
            builder.append(s.toHTML());
        }
        builder.append("</div>");

        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(getId());
        parcel.writeInt(getLevel());
        parcel.writeString(getHeading());
        parcel.writeString(getAnchor());
        parcel.writeString(getContent());
        parcel.writeList(subSections);
    }

    public static final Parcelable.Creator<Section> CREATOR
            = new Parcelable.Creator<Section>() {
        public Section createFromParcel(Parcel in) {
            return new Section(in);
        }

        public Section[] newArray(int size) {
            return new Section[size];
        }
    };
}
