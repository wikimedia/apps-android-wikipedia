package org.wikipedia.feed.onthisday;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.L10nUtil;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OnThisDayCard extends WikiSiteCard {
    private int nextYear;
    private Calendar date;
    private FeedAdapter.Callback callback;
    private OnThisDay.Event eventShownOnCard;
    private int age;

    public OnThisDayCard(@NonNull List<OnThisDay.Event> events, @NonNull WikiSite wiki, int age) {
        super(wiki);
        this.date = DateUtil.getDefaultDateFor(age);
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
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.on_this_day_card_title);
    }

    @Override @NonNull public String subtitle() {
        return DateUtil.getFeedCardShortDateString(date);
    }

    @NonNull
    public String footerActionText() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.more_events_text);
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

    @Nullable public List<PageSummary> pages() {
        return eventShownOnCard.pages();
    }

    int getAge() {
        return age;
    }

    @Override protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(date.getTime().getTime()) + wikiSite().hashCode();
    }
}
