package org.wikipedia.feed.onthisday;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
        Collections.sort(allEvents, Comparator.comparingInt(Event::year).reversed());
        return allEvents;
    }

    public static class Event {
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String text;
        @SuppressWarnings("unused") private int year;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private List<PageSummary> pages;

        @NonNull
        public CharSequence text() {
            List<String> pageTitles = new ArrayList<>();
            for (PageSummary page : pages) {
                pageTitles.add((StringUtil.fromHtml(page.getDisplayTitle())).toString());
            }
            return StringUtil.boldenSubstrings(text, pageTitles);
        }

        public int year() {
            return year;
        }

        @Nullable
        public List<PageSummary> pages() {
            pages.removeIf(Objects::isNull);
            return pages;
        }
    }

    public void setSelected(@Nullable List<Event> selected) {
        this.selected = selected;
    }

}
