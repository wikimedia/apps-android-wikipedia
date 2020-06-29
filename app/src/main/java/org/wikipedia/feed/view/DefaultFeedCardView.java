package org.wikipedia.feed.view;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.WikiCardView;

import static org.wikipedia.util.L10nUtil.isLangRTL;

public abstract class DefaultFeedCardView<T extends Card> extends WikiCardView implements FeedCardView<T> {
    @Nullable private T card;
    @Nullable private FeedAdapter.Callback callback;

    public DefaultFeedCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull T card) {
        this.card = card;
    }

    @Nullable @Override public T getCard() {
        return card;
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    protected void setAllowOverflow(boolean enabled) {
        setClipChildren(!enabled);
    }

    @Nullable protected FeedAdapter.Callback getCallback() {
        return callback;
    }

    protected void setLayoutDirectionByWikiSite(@NonNull WikiSite wiki, @NonNull View rootView) {
        rootView.setLayoutDirection(isLangRTL(wiki.languageCode()) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);
    }
}
