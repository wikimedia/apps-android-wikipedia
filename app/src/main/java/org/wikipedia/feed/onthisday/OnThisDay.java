package org.wikipedia.feed.onthisday;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.json.annotations.Required;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
        Collections.sort(allEvents, (e1, e2) -> Integer.compare(e2.year(), e1.year()));
        return allEvents;
    }

    public static class Event {
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String text;
        @SuppressWarnings("unused") private int year;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private List<PageSummary> pages;

        @NonNull
        public CharSequence text() {
            return text;
        }

        public int year() {
            return year;
        }

        @Nullable
        public List<PageSummary> pages() {
            Iterator iterator = pages.iterator();
            while ((iterator.hasNext())) {
                if (iterator.next() == null) {
                    iterator.remove();
                }
            }
            return pages;
        }
    }

    public void setSelected(@Nullable List<Event> selected) {
        this.selected = selected;
    }

}
