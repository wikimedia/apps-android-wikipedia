package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;

import java.util.ArrayList;
import java.util.List;

public abstract class FeedCoordinatorBase {

    public interface FeedUpdateListener {
        void update(List<Card> cards);
    }

    @NonNull private Context context;
    @Nullable private FeedUpdateListener updateListener;
    @NonNull private final List<Card> cards = new ArrayList<>();
    private int currentAge;
    private List<FeedClient> pendingClients = new ArrayList<>();
    private FeedClient.Callback exhaustionClientCallback = new ExhaustionClientCallback();

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
        currentAge = 0;
        for (FeedClient client : pendingClients) {
            client.cancel();
        }
        pendingClients.clear();
        cards.clear();
    }

    public void more(Site site) {
        if (cards.size() > 0) {
            currentAge++;
        }

        buildScript(site);
        requestNextCard();
    }

    protected abstract void buildScript(Site site);

    protected void addPendingClient(FeedClient client) {
        pendingClients.add(client);
    }

    private void requestNextCard() {
        if (pendingClients.isEmpty()) {
            return;
        }
        pendingClients.remove(0).request(context, currentAge, exhaustionClientCallback);
    }

    private class ExhaustionClientCallback implements FeedClient.Callback {
        @Override
        public void success(@NonNull List<Card> cardList) {
            cards.addAll(cardList);
            if (updateListener != null) {
                updateListener.update(cards);
            }
            requestNextCard();
        }

        @Override
        public void error(@NonNull Throwable caught) {
            requestNextCard();
        }
    }
}
