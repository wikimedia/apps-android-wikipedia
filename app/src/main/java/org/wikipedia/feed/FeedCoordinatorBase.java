package org.wikipedia.feed;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.accessibility.AccessibilityCard;
import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.announcement.AnnouncementClient;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.dayheader.DayHeaderCard;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsCard;
import org.wikipedia.feed.offline.OfflineCard;
import org.wikipedia.feed.onthisday.OnThisDayCard;
import org.wikipedia.feed.progress.ProgressCard;
import org.wikipedia.feed.suggestededits.SuggestedEditsFeedClient;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FeedCoordinatorBase {
    private static final int MAX_HIDDEN_CARDS = 100;

    public interface FeedUpdateListener {
        void insert(Card card, int pos);
        void remove(Card card, int pos);
        void finished(boolean shouldUpdatePreviousCard);
    }

    @NonNull private Context context;
    @Nullable private WikiSite wiki;
    @Nullable private FeedUpdateListener updateListener;
    @NonNull private final List<Card> cards = new ArrayList<>();
    private int currentAge;
    private List<FeedClient> pendingClients = new ArrayList<>();
    private FeedClient.Callback callback = new ClientRequestCallback();
    private Card progressCard = new ProgressCard();
    private int currentDayCardAge = -1;

    private Set<String> hiddenCards = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        @Override
        public boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_HIDDEN_CARDS;
        }
    });

    public FeedCoordinatorBase(@NonNull Context context) {
        this.context = context;
        updateHiddenCards();
    }

    public void updateHiddenCards() {
        hiddenCards.clear();
        hiddenCards.addAll(Prefs.getHiddenCards());
    }

    @NonNull
    public List<Card> getCards() {
        return cards;
    }

    public void setFeedUpdateListener(@Nullable FeedUpdateListener listener) {
        updateListener = listener;
    }

    public void reset() {
        wiki = null;
        currentAge = 0;
        currentDayCardAge = -1;
        for (FeedClient client : pendingClients) {
            client.cancel();
        }
        pendingClients.clear();
        cards.clear();
    }

    public void incrementAge() {
        currentAge++;
    }

    public void more(@NonNull WikiSite wiki) {
        this.wiki = wiki;

        if (cards.size() == 0) {
            requestProgressCard();
        }

        if (DeviceUtil.isAccessibilityEnabled()) {
            removeAccessibilityCard();
        }

        buildScript(currentAge);
        requestCard(wiki);
    }

    public boolean finished() {
        return pendingClients.isEmpty();
    }

    public int getAge() {
        return currentAge;
    }

    public int dismissCard(@NonNull Card card) {
        int position = cards.indexOf(card);
        if (card.type() == CardType.RANDOM) {
            FeedContentType.RANDOM.setEnabled(false);
            FeedContentType.saveState();
        } else if (card.type() == CardType.MAIN_PAGE) {
            FeedContentType.MAIN_PAGE.setEnabled(false);
            FeedContentType.saveState();
        } else {
            addHiddenCard(card);
        }
        removeCard(card, position);
        card.onDismiss();
        return position;
    }

    public void undoDismissCard(@NonNull Card card, int position) {
        if (card.type() == CardType.RANDOM) {
            FeedContentType.RANDOM.setEnabled(true);
            FeedContentType.saveState();
        } else if (card.type() == CardType.MAIN_PAGE) {
            FeedContentType.MAIN_PAGE.setEnabled(true);
            FeedContentType.saveState();
        } else {
            unHideCard(card);
        }
        insertCard(card, position);
        card.onRestore();
    }

    protected abstract void buildScript(int age);

    void addPendingClient(@Nullable FeedClient client) {
        if (client != null) {
            pendingClients.add(client);
        }
    }

    void conditionallyAddPendingClient(@Nullable FeedClient client, boolean condition) {
        if (condition && client != null) {
            pendingClients.add(client);
        }
    }

    // Call to kick off the request chain or to retry a failed request.  To move to the next pending
    // client, call requestNextCard.
    private void requestCard(@NonNull WikiSite wiki) {
        if (pendingClients.isEmpty()) {
            removeProgressCard();
            return;
        }
        pendingClients.get(0).request(context, wiki, currentAge, callback);
    }

    private void requestNextCard(@NonNull WikiSite wiki) {
        if (!pendingClients.isEmpty()) {
            pendingClients.remove(0);
        }
        if (!(getLastCard() instanceof ProgressCard) && shouldShowProgressCard(pendingClients.get(0))) {
            requestProgressCard();
        }
        requestCard(wiki);
    }

    void requestOfflineCard() {
        if (!(getLastCard() instanceof OfflineCard)) {
            appendCard(new OfflineCard());
        }
    }

    void removeOfflineCard() {
        if (getLastCard() instanceof OfflineCard) {
            dismissCard(getLastCard());
        }
    }

    private Card getLastCard() {
        return cards.size() > 1 ? cards.get(cards.size() - 1) : null;
    }

    private void requestProgressCard() {
        if (!(getLastCard() instanceof ProgressCard)) {
            appendCard(progressCard);
        }
    }

    private void removeProgressCard() {
        removeCard(progressCard, cards.indexOf(progressCard));
    }

    private void setOfflineState() {
        removeProgressCard();
        appendCard(new OfflineCard());
    }

    private void removeAccessibilityCard() {
        if (getLastCard() instanceof AccessibilityCard) {
            removeCard(getLastCard(), cards.indexOf(getLastCard()));
            getLastCard().onDismiss();
            // TODO: possible on optimization if automatically scroll up to the next card.
        }
    }

    private class ClientRequestCallback implements FeedClient.Callback {
        @Override public void success(@NonNull List<? extends Card> cardList) {
            boolean atLeastOneAppended = false;
            for (Card card : cardList) {
                if (!isCardHidden(card)) {
                    appendCard(card);
                    atLeastOneAppended = true;
                }
            }
            //noinspection ConstantConditions
            requestNextCard(wiki);
            if (pendingClients.isEmpty() && updateListener != null) {
                updateListener.finished(!atLeastOneAppended);
            }
        }

        @Override public void error(@NonNull Throwable caught) {
            if (ThrowableUtil.isOffline(caught)) {
                setOfflineState();
            } else {
                //noinspection ConstantConditions
                requestNextCard(wiki);
                L.w(caught);
            }
        }
    }

    private void appendCard(@NonNull Card card) {
        int progressPos = cards.indexOf(progressCard);
        int pos = progressPos >= 0 ? progressPos : cards.size();

        if (isDailyCardType(card) && currentDayCardAge < currentAge) {
            currentDayCardAge = currentAge;
            insertCard(new DayHeaderCard(currentDayCardAge), pos++);
        }

        insertCard(card, pos);
    }

    private void insertCard(@NonNull Card card, int position) {
        if (position < 0) {
            return;
        }
        cards.add(position, card);
        if (updateListener != null) {
            updateListener.insert(card, position);
        }
    }

    private void removeCard(@NonNull Card card, int position) {
        if (position < 0) {
            return;
        }
        cards.remove(card);
        if (updateListener != null) {
            updateListener.remove(card, position);
        }
    }

    private void addHiddenCard(@NonNull Card card) {
        hiddenCards.add(card.getHideKey());
        Prefs.setHiddenCards(hiddenCards);
    }

    private boolean isCardHidden(@NonNull Card card) {
        return hiddenCards.contains(card.getHideKey());
    }

    private void unHideCard(@NonNull Card card) {
        hiddenCards.remove(card.getHideKey());
        Prefs.setHiddenCards(hiddenCards);
    }

    private boolean isDailyCardType(@NonNull Card card) {
        return card instanceof NewsCard || card instanceof OnThisDayCard
                || card instanceof MostReadListCard || card instanceof FeaturedArticleCard
                || card instanceof FeaturedImageCard;
    }

    private boolean shouldShowProgressCard(@NonNull FeedClient pendingClient) {
        return pendingClient instanceof SuggestedEditsFeedClient
                || pendingClient instanceof AnnouncementClient
                || pendingClient instanceof BecauseYouReadClient;
    }
}
