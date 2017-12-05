package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.util.StringUtil;

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
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private List<RbPageSummary> pages;

        @NonNull
        public CharSequence text() {
            List<String> pageTitles = new ArrayList<>();
            for (RbPageSummary page : pages) {
                pageTitles.add((StringUtil.fromHtml(StringUtils.defaultString(page.getNormalizedTitle()))).toString());
            }
            return StringUtil.boldenSubstrings(text, pageTitles);
        }

        public int year() {
            return year;
        }

        @Nullable
        public List<RbPageSummary> pages() {
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
