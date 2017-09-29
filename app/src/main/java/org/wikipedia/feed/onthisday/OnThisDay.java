package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.model.Thumbnail;

import java.util.List;

public class OnThisDay {

    private List<Event> events;

    @NonNull public List<Event> events() {
        return events;
    }

    static class Event {
        private String text;
        private int year;
        private List<Page> pages;

        public String text() {
            return text;
        }

        public int year() {
            return year;
        }

        public List<Page> pages() {
            return pages;
        }

    }

    static class Page {
        private String title;
        @SerializedName("displaytitle") private String displayTitle;
        @SerializedName("extract") private String text;
        private Thumbnail thumbnail;

        @Nullable public String displayTitle() {
            return displayTitle;
        }

        @Nullable public String text() {
            return text;
        }

        @Nullable public Thumbnail thumbnail() {
            return thumbnail;
        }

        @Nullable public String title() {
            return title;
        }
    }
}
