package org.wikipedia.feed.mainpage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.L10nUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainPageCardView extends DefaultFeedCardView<MainPageCard> {
    @BindView(R.id.view_static_card_header) CardHeaderView headerView;
    @BindView(R.id.view_static_card_footer) CardFooterView footerView;

    public MainPageCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_static_card, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull final MainPageCard card) {
        super.setCard(card);

        headerView.setTitle(L10nUtil.getStringForArticleLanguage(getCard().wikiSite().languageCode(), R.string.view_main_page_card_title))
                .setLangCode(getCard().wikiSite().languageCode())
                .setCard(getCard())
                .setCallback(getCallback());

        footerView.setCallback(this::goToMainPage);
        footerView.setFooterActionText(getContext().getString(R.string.view_main_page_card_action));
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    private void goToMainPage() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onSelectPage(getCard(),
                    new HistoryEntry(new PageTitle(SiteInfoClient.getMainPageForLang(getCard().wikiSite().languageCode()), getCard().wikiSite()),
                            HistoryEntry.SOURCE_FEED_MAIN_PAGE));
        }
    }
}
