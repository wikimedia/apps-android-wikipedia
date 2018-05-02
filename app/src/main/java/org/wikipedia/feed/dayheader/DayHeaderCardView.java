package org.wikipedia.feed.dayheader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedCardView;

public class DayHeaderCardView extends FrameLayout implements FeedCardView<Card> {
    private Card card;
    private TextView dayTextView;

    public DayHeaderCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_day_header, this);
        dayTextView = findViewById(R.id.day_header_text);
    }

    @Override public void setCard(@NonNull Card card) {
        this.card = card;
        dayTextView.setText(card.title());
    }

    @Override public Card getCard() {
        return card;
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) { }
}
