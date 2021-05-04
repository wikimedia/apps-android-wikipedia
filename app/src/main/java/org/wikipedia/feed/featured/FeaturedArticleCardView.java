package org.wikipedia.feed.featured;

import android.content.Context;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.LongPressMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.views.ImageZoomHelper;
import org.wikipedia.views.WikiArticleCardView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class FeaturedArticleCardView extends DefaultFeedCardView<FeaturedArticleCard> {

    @BindView(R.id.view_featured_article_card_header) CardHeaderView headerView;
    @BindView(R.id.view_featured_article_card_footer) CardFooterView footerView;
    @BindView(R.id.view_wiki_article_card) WikiArticleCardView wikiArticleCardView;
    @BindView(R.id.view_featured_article_card_content_container) View contentContainerView;

    public static final int EXTRACT_MAX_LINES = 8;

    public FeaturedArticleCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_article, this);
        ButterKnife.bind(this);
    }

    public void setCard(@NonNull FeaturedArticleCard card) {
        super.setCard(card);
        setLayoutDirectionByWikiSite(card.wikiSite(), contentContainerView);
        String articleTitle = card.articleTitle();
        String articleSubtitle = card.articleSubtitle();
        String extract = card.extract();
        Uri imageUri = card.image();

        articleTitle(articleTitle);
        articleSubtitle(articleSubtitle);
        extract(extract);
        image(imageUri);

        header();
        footer();
    }

    @OnClick({R.id.view_featured_article_card_content_container})
    void onCardClick() {
        if (getCallback() != null && getCard() != null) {
                getCallback().onSelectPage(getCard(), getCard().historyEntry(), wikiArticleCardView.getSharedElements());
        }
    }

    @OnLongClick(R.id.view_featured_article_card_content_container)
    boolean onLongClick(View view) {
        if (ImageZoomHelper.isZooming()) {
            // Dispatch a fake CANCEL event to the container view, so that the long-press ripple is cancelled.
            contentContainerView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0));
        } else if (getCallback() != null && getCard() != null) {
                    new LongPressMenu(view, true, new LongPressMenu.Callback() {
                @Override
                public void onOpenLink(@NonNull HistoryEntry entry) {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onSelectPage(getCard(), entry, false);
                    }
                }

                @Override
                public void onOpenInNewTab(@NonNull HistoryEntry entry) {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onSelectPage(getCard(), entry, true);
                    }
                }

                @Override
                public void onAddRequest(@NonNull HistoryEntry entry, boolean addToDefault) {
                    if (getCallback() != null) {
                        getCallback().onAddPageToList(entry, addToDefault);
                    }
                }

                @Override
                public void onMoveRequest(@Nullable ReadingListPage page, @NonNull HistoryEntry entry) {
                    if (getCallback() != null) {
                        getCallback().onMovePageToList(page.getListId(), entry);
                    }
                }
            }).show(getCard().historyEntry());
        }
        return false;
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    private void articleTitle(@NonNull String articleTitle) {
        wikiArticleCardView.setTitle(articleTitle);
    }

    private void articleSubtitle(@Nullable String articleSubtitle) {
        wikiArticleCardView.setDescription(articleSubtitle);
    }

    private void extract(@Nullable String extract) {
        wikiArticleCardView.setExtract(extract, EXTRACT_MAX_LINES);
    }

    private void header() {
        if (getCard() == null) {
            return;
        }
        headerView.setTitle(getCard().title())
                .setLangCode(getCard().wikiSite().languageCode())
                .setCard(getCard())
                .setCallback(getCallback());
    }

    private void footer() {
        if (getCard() == null) {
            return;
        }
        footerView.setCallback(getFooterCallback());
        footerView.setFooterActionText(getCard().footerActionText(), getCard().wikiSite().languageCode());
    }

    private void image(@Nullable Uri uri) {
        wikiArticleCardView.setImageUri(uri, false);
        if (uri != null) {
            ImageZoomHelper.setViewZoomable(wikiArticleCardView.getImageView());
        }
    }

    public CardFooterView.Callback getFooterCallback() {
        return () -> {
            if (getCallback() != null && getCard() != null) {
                getCallback().onSelectPage(getCard(), new HistoryEntry(
                        new PageTitle(SiteInfoClient.getMainPageForLang(getCard().wikiSite().languageCode()),
                                getCard().wikiSite()), getCard().historyEntry().getSource()), false);
            }
        };
    }
}
