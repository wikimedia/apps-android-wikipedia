package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.json.annotations.Required;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OnThisDay {

    @SuppressWarnings("unused") @Nullable private List<Event> selected;
    @SuppressWarnings("unused") @Nullable private List<Event> events;
    @SuppressWarnings("unused") @Nullable private List<Event> births;
    @SuppressWarnings("unused") @Nullable private List<Event> deaths;
    @SuppressWarnings("unused") @Nullable private List<Event> holidays;

    @NonNull public List<Event> selectedEvents() {
        return selected != null ? selected : Collections.emptyList();
    }

    @NonNull public List<Event> events() {
        ArrayList<Event> allEvents = new ArrayList<>();
        if (events != null) {
            allEvents.addAll(events);
        }
        if (births != null) {
            allEvents.addAll(births);
        }
        if (deaths != null) {
            allEvents.addAll(deaths);
        }
        if (holidays != null) {
            allEvents.addAll(holidays);
        }
        Collections.sort(allEvents, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Integer.compare(e2.year(), e1.year());
            }
        });
        return allEvents;
    }

    static class Event {
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String text;
        @SuppressWarnings("unused") private int year;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private List<RbPageSummary> pages;

        @NonNull public String text() {
            return text;
        }

        public int year() {
            return year;
        }

        @NonNull public List<RbPageSummary> pages() {
            return pages;
        }
    }
}
