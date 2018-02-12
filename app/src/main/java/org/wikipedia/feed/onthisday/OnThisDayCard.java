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
    private FeedAdapter.Callback callback;
    private WikiSite wiki;
    private OnThisDay.Event eventShownOnCard;
    private int age;

    public OnThisDayCard(@NonNull List<OnThisDay.Event> events, @NonNull WikiSite wiki, int age) {
        super();
        this.date = DateUtil.getDefaultDateFor(age);
        this.wiki = wiki;
        this.age = age;
        int randomIndex = 0;
        if (events.size() > 1) {
            randomIndex = new Random().nextInt(events.size() - 1);
        }
        eventShownOnCard = events.get(randomIndex);
        this.nextYear = randomIndex + 1 < events.size() ? events.get(randomIndex + 1).year() : eventShownOnCard.year();
    }

    public FeedAdapter.Callback getCallback() {
        return callback;
    }

    public void setCallback(FeedAdapter.Callback callback) {
        this.callback = callback;
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
