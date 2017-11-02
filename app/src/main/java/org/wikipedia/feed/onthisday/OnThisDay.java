package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.model.Thumbnail;

import java.util.ArrayList;
import java.util.List;

public class OnThisDay {

    private List<Event> selected;
    private List<Event> events;
    private List<Event> births;
    private List<Event> deaths;
    private List<Event> holidays;

    @NonNull public List<Event> selectedEvents() {
        return selected;
    }
    @NonNull public List<Event> events() {
        ArrayList<Event> allEvents = new ArrayList<>();
        allEvents.addAll(events);
        allEvents.addAll(births);
        allEvents.addAll(deaths);
        allEvents.addAll(holidays);
        return allEvents;
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
