package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DateUtil;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class OnThisDayCard extends Card {
    private int nextYear;
    private Calendar date;
    private OnThisDay onThisDay;
    private FeedAdapter.Callback callback;
    private WikiSite wiki;
    private OnThisDay.Event eventShownOnCard;
    private int age;

    public OnThisDayCard(@NonNull OnThisDay onThisDay, @NonNull WikiSite wiki, int age) {
        super();
        this.onThisDay = onThisDay;
        int randomIndex = new Random().nextInt(onThisDay.selectedEvents().size() - 1);
        eventShownOnCard = onThisDay.selectedEvents().get(randomIndex);
        this.date = DateUtil.getDefaultDateFor(age);
        this.nextYear = onThisDay.selectedEvents().get(randomIndex + 1).year();
        this.wiki = wiki;
        this.age = age;
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
        return onThisDay.selectedEvents();
    }

    @Override @NonNull public CardType type() {
        return CardType.ON_THIS_DAY;
    }

    @Override @NonNull public String title() {
        return WikipediaApp.getInstance().getString(R.string.on_this_day_card_title);
    }

    @Override @NonNull public String subtitle() {
        return DateUtil.getFeedCardShortDateString(date);
    }

    @NonNull String dayString() {
        return DateUtil.getMonthOnlyDateString(date.getTime());
    }

    @NonNull public CharSequence text() {
        return eventShownOnCard.text();
    }

    public int year() {
        return eventShownOnCard.year();
    }

    @NonNull public Calendar date() {
        return date;
    }

    int nextYear() {
        return nextYear;
    }

    @NonNull public WikiSite wiki() {
        return wiki;
    }

    @Nullable public List<RbPageSummary> pages() {
        return eventShownOnCard.pages();
    }

    int getAge() {
        return age;
    }
}
