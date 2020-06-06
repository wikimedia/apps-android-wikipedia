package org.wikipedia.feed.onthisday;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DateUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

public class OnThisDayCard extends WikiSiteCard {
    private int nextYear;
    private LocalDateTime date;
    private FeedAdapter.Callback callback;
    private OnThisDay.Event eventShownOnCard;
    private int age;

    public OnThisDayCard(@NonNull List<OnThisDay.Event> events, @NonNull WikiSite wiki, int age) {
        super(wiki);
        this.date = LocalDateTime.now().minusDays(age);
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
        return DateUtil.getFeedCardShortDateString(GregorianCalendar.from(date.atZone(ZoneId.systemDefault())));
    }

    @NonNull String dayString() {
        return DateUtil.getMonthOnlyDateString(Date.from(date.atZone(ZoneId.systemDefault()).toInstant()));
    }

    @NonNull public CharSequence text() {
        return eventShownOnCard.text();
    }

    public int year() {
        return eventShownOnCard.year();
    }

    @NonNull public LocalDateTime date() {
        return date;
    }

    int nextYear() {
        return nextYear;
    }

    @Nullable public List<PageSummary> pages() {
        return eventShownOnCard.pages();
    }

    int getAge() {
        return age;
    }

    @Override protected int dismissHashCode() {
        return date.get(ChronoField.EPOCH_DAY) + wikiSite().hashCode();
    }
}
