package org.wikipedia.feed.featured;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DimenUtil;
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

    public static final float SUM_OF_CARD_HORIZONTAL_MARGINS = DimenUtil.dpToPx(24f);

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

    @OnClick({R.id.view_wiki_article_card})
    void onCardClick() {
        if (getCallback() != null && getCard() != null) {
                getCallback().onSelectPage(getCard(), getCard().historyEntry(), wikiArticleCardView.getSharedElements());
        }
    }

    @OnLongClick(R.id.view_featured_article_card_content_container)
    boolean onLongClick(View view) {
        if (getCallback() != null && getCard() != null) {
            new ReadingListBookmarkMenu(view, true, new ReadingListBookmarkMenu.Callback() {
                @Override
                public void onAddRequest(boolean addToDefault) {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onAddPageToList(getCard().historyEntry(), addToDefault);
                    }
                }

                @Override
                public void onMoveRequest(@Nullable ReadingListPage page) {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onMovePageToList(page.listId(), getCard().historyEntry());
                    }
                }

                @Override
                public void onDeleted(@Nullable ReadingListPage page) {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onRemovePageFromList(getCard().historyEntry());
                    }
                }

                @Override
                public void onShare() {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onSharePage(getCard().historyEntry());
                    }
                }
            }).show(getCard().historyEntry().getTitle());
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
        wikiArticleCardView.setExtract(extract);
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
        footerView.setFooterActionText(getCard().footerActionText());
    }

    private void image(@Nullable Uri uri) {
        if (uri == null) {
            wikiArticleCardView.getImageContainer().setVisibility(GONE);
        } else {
            wikiArticleCardView.getImageContainer().setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (DimenUtil.leadImageHeightForDevice(getContext()) - SUM_OF_CARD_HORIZONTAL_MARGINS)));
            wikiArticleCardView.getImageContainer().setVisibility(VISIBLE);
            wikiArticleCardView.getImageView().loadImage(uri);
            ImageZoomHelper.setViewZoomable(wikiArticleCardView.getImageView());
        }
    }

    public CardFooterView.Callback getFooterCallback() {
        return () -> {
            if (getCallback() != null && getCard() != null) {
                getCallback().onSelectPage(getCard(), new HistoryEntry(
                        new PageTitle(SiteInfoClient.getMainPageForLang(getCard().wikiSite().languageCode()),
                                getCard().wikiSite()), getCard().historyEntry().getSource()));
            }
        };
    }
}
