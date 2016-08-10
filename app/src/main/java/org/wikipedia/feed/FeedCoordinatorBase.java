package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.progress.ProgressCard;
import org.wikipedia.settings.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FeedCoordinatorBase {
    private static final int MAX_HIDDEN_CARDS = 100;

    public interface FeedUpdateListener {
        void update(List<Card> cards);
    }

    @NonNull private Context context;
    @Nullable private Site site;
    @Nullable private FeedUpdateListener updateListener;
    @NonNull private final List<Card> cards = new ArrayList<>();
    private int currentAge;
    private List<FeedClient> pendingClients = new ArrayList<>();
    private FeedClient.Callback exhaustionClientCallback = new ExhaustionClientCallback();
    private Card progressCard = new ProgressCard();

    private Set<String> hiddenCards = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        @Override
        public boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_HIDDEN_CARDS;
        }
    });

    public FeedCoordinatorBase(@NonNull Context context) {
        this.context = context;
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
        site = null;
        currentAge = 0;
        for (FeedClient client : pendingClients) {
            client.cancel();
        }
        pendingClients.clear();
        cards.clear();
        appendProgressCard(cards);
    }

    public void more(@NonNull Site site) {
        this.site = site;
        if (cards.size() > 1) {
            currentAge++;
        }

        buildScript(currentAge);
        requestNextCard(site);
    }

    public boolean finished() {
        return pendingClients.isEmpty();
    }

    public int getAge() {
        return currentAge;
    }

    public int dismissCard(@NonNull Card card) {
        int position = cards.indexOf(card);
        cards.remove(card);
        addHiddenCard(card);
        if (updateListener != null) {
            updateListener.update(cards);
        }
        return position;
    }

    public void insertCard(@NonNull Card card, int position) {
        cards.add(position, card);
        unHideCard(card);
        if (updateListener != null) {
            updateListener.update(cards);
        }
    }

    protected abstract void buildScript(int age);

    protected void addPendingClient(FeedClient client) {
        pendingClients.add(client);
    }

    private void requestNextCard(@NonNull Site site) {
        if (pendingClients.isEmpty()) {
            return;
        }
        pendingClients.remove(0).request(context, site, currentAge, exhaustionClientCallback);
    }

    private class ExhaustionClientCallback implements FeedClient.Callback {
        @Override
        public void success(@NonNull List<? extends Card> cardList) {
            for (Card card : cardList) {
                if (!isCardHidden(card)) {
                    cards.add(card);
                }
            }
            appendProgressCard(cards);
            if (updateListener != null) {
                updateListener.update(cards);
            }
            //noinspection ConstantConditions
            requestNextCard(site);
        }

        @Override
        public void error(@NonNull Throwable caught) {
            //noinspection ConstantConditions
            requestNextCard(site);
        }
    }

    private void appendProgressCard(List<Card> cards) {
        cards.remove(progressCard);
        cards.add(progressCard);
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
}
