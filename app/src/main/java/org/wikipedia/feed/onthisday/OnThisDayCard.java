package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DateUtil;

import java.util.List;

public class OnThisDayCard extends Card {
    private int nextYear;
    private UtcDate date;
    private OnThisDay onThisDay;
    private FeedAdapter.Callback callback;
    private WikiSite wiki;
    private OnThisDay.Event eventShownOnCard;

    OnThisDayCard(@NonNull OnThisDay onThisDay, @NonNull OnThisDay.Event event, int nextYear, @NonNull UtcDate date, @NonNull WikiSite wiki) {
        super();
        this.onThisDay = onThisDay;
        eventShownOnCard = event;
        this.date = date;
        this.nextYear = nextYear;
        this.wiki = wiki;
    }

    @NonNull public OnThisDay onthisday() {
        return onThisDay;
    }

    public FeedAdapter.Callback getCallback() {
        return callback;
    }

    public void setCallback(FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    @NonNull public List<OnThisDay.Event> events() {
        return onThisDay.events();
    }

    @Override @NonNull public CardType type() {
        return CardType.ON_THIS_DAY;
    }

    @Override @NonNull public String title() {
        return WikipediaApp.getInstance().getString(R.string.on_this_day_card_title);
    }

    @Override @NonNull public String subtitle() {
        return DateUtil.getFeedCardDateString(date().baseCalendar());
    }

    @NonNull public String text() {
        return eventShownOnCard.text();
    }

    public int year() {
        return eventShownOnCard.year();
    }

    @NonNull public UtcDate date() {
        return date;
    }

    int nextYear() {
        return nextYear;
    }

    @NonNull public WikiSite wiki() {
        return wiki;
    }

    @NonNull public List<OnThisDay.Page> pages() {
        return eventShownOnCard.pages();
    }

}
