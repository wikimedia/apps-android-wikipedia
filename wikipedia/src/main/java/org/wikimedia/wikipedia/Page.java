package org.wikimedia.wikipedia;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Represents a particular page along with its full contents.
 */
public class Page implements Parcelable {
    private final PageTitle title;
    private final ArrayList<Section> sections;

    public Page(PageTitle title, ArrayList<Section> sections) {
        this.title = title;
        this.sections = sections;
    }

    public Page(Parcel in) {
        title = in.readParcelable(PageTitle.class.getClassLoader());
        sections = in.readArrayList(Section.class.getClassLoader());
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

    public static final Parcelable.Creator<Page> CREATOR
            = new Parcelable.Creator<Page>() {
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        public Page[] newArray(int size) {
            return new Page[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(title, flags);
        parcel.writeList(sections);
    }
}
