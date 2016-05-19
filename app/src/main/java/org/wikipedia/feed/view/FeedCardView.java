package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;

import org.wikipedia.R;
import org.wikipedia.feed.model.FeedCard;
import org.wikipedia.util.DimenUtil;

import butterknife.ButterKnife;

public class FeedCardView extends CardView {
    public FeedCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_feed_card, this);
        ButterKnife.bind(this);
    }

    public void update(@NonNull FeedCard card) {
        // TODO: [Feed] replace with real data.
        setBackgroundColor(card.color());
        setMinimumHeight(card.height());
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setLayoutAttrs();
    }

    private void setLayoutAttrs() {
        final int margin = Math.round(DimenUtil.getDimension(R.dimen.margin));
        MarginLayoutParams params = ((MarginLayoutParams) getLayoutParams());
        params.topMargin = margin;
        params.rightMargin = margin;
        params.bottomMargin = margin;
        params.leftMargin = margin;
    }
}