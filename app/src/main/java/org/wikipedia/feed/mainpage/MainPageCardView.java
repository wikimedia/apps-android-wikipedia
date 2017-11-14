package org.wikipedia.feed.mainpage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;

import java.text.DateFormat;
import java.util.Date;

public class MainPageCardView extends StaticCardView<MainPageCard> {
    public MainPageCardView(@NonNull Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull final MainPageCard card) {
        super.setCard(card);
        setTitle(getString(R.string.view_main_page_card_title));
        setSubtitle(String.format(getString(R.string.view_main_page_card_subtitle),
                DateFormat.getDateInstance().format(new Date())));
        setIcon(R.drawable.ic_today_24dp);
        setContainerBackground(R.color.green50);
        setAction(R.drawable.ic_arrow_forward_black_24dp, R.string.view_main_page_card_action);
    }

    protected void onContentClick(View v) {
        goToMainPage();
    }

    protected void onActionClick(View v) {
        goToMainPage();
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
    }

    private void goToMainPage() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onSelectPage(getCard(),
                    new HistoryEntry(MainPageClient.getMainPageTitle(),
                            HistoryEntry.SOURCE_FEED_MAIN_PAGE));
        }
    }
}
