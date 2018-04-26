package org.wikipedia.feed.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.ResourceUtil;

import static org.wikipedia.util.L10nUtil.isLangRTL;

public abstract class DefaultFeedCardView<T extends Card> extends CardView implements FeedCardView<T> {
    @Nullable private T card;
    @Nullable private FeedAdapter.Callback callback;

    public DefaultFeedCardView(Context context) {
        super(context);
        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color));
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(!enabled);
        }
    }

    @Nullable protected FeedAdapter.Callback getCallback() {
        return callback;
    }

    protected void setLayoutDirectionByWikiSite(@NonNull WikiSite wiki, @NonNull View rootView) {
        rootView.setLayoutDirection(isLangRTL(wiki.languageCode()) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);
    }
}
