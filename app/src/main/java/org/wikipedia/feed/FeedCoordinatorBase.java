package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.progress.ProgressCard;

import java.util.ArrayList;
import java.util.List;

public abstract class FeedCoordinatorBase {

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

    public FeedCoordinatorBase(@NonNull Context context) {
        this.context = context;
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
        if (updateListener != null) {
            updateListener.update(cards);
        }
        return position;
    }

    public void insertCard(@NonNull Card card, int position) {
        cards.add(position, card);
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
            cards.addAll(cardList);
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
}
