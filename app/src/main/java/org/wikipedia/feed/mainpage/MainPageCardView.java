package org.wikipedia.feed.mainpage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.staticdata.MainPageNameData;

import java.text.DateFormat;
import java.util.Date;

public class MainPageCardView extends StaticCardView {
    @Nullable private FeedViewCallback callback;

    public MainPageCardView(@NonNull Context context) {
        super(context);
    }

    public void set(@NonNull final MainPageCard card) {
        setTitle(getString(R.string.view_main_page_card_title));
        setSubtitle(String.format(getString(R.string.view_main_page_card_subtitle),
                DateFormat.getDateInstance().format(new Date())));
        setIcon(R.drawable.icon_feed_today);
        setOnClickListener(new CallbackAdapter(callback));
    }

    @NonNull public MainPageCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    private static class CallbackAdapter implements OnClickListener {
        @NonNull private WikipediaApp app = WikipediaApp.getInstance();
        @Nullable private final FeedViewCallback callback;

        CallbackAdapter(@Nullable FeedViewCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onClick(View view) {
            if (callback != null) {
                PageTitle title = new PageTitle(MainPageNameData
                        .valueFor(app.getAppOrSystemLanguageCode()), app.getSite());
                callback.onSelectPage(new HistoryEntry(title, HistoryEntry.SOURCE_FEED_MAIN_PAGE));
            }
        }
    }
}
