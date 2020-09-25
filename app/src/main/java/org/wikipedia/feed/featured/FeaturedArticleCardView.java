package org.wikipedia.feed.featured;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ImageZoomHelper;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class FeaturedArticleCardView extends DefaultFeedCardView<FeaturedArticleCard> {

    @BindView(R.id.view_featured_article_card_header) CardHeaderView headerView;
    @BindView(R.id.view_featured_article_card_footer) CardFooterView footerView;
    @BindView(R.id.view_featured_article_card_image_container) View imageContainerView;
    @BindView(R.id.view_featured_article_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_featured_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_featured_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_featured_article_card_extract) TextView extractView;
    @BindView(R.id.view_featured_article_card_content_container) View contentContainerView;
    @BindView(R.id.view_featured_article_card_content) View contentView;
    @BindView(R.id.view_featured_article_card_transition_image) ImageView transitionImageView;
    public FeaturedArticleCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_article, this);
        ButterKnife.bind(this);
        ImageZoomHelper.setViewZoomable(imageView);
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
        resetTransitionImageView();

        header(card);
        footer();
    }

    @OnClick({R.id.view_featured_article_card_image, R.id.view_featured_article_card_content_container})
    void onCardClick() {
        if (getCallback() != null && getCard() != null) {
            ViewUtil.setCachedBitmap(ViewUtil.getBitmapFromView(contentView));
            ViewUtil.setViewCachedBitmap(transitionImageView);
            transitionImageView.setVisibility(View.VISIBLE);
            transitionImageView.post(() -> {
                if (!isAttachedToWindow()) {
                    return;
                }
                getCallback().onSelectPage(getCard(), getCard().historyEntry(), transitionImageView);
            });
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

    private void resetTransitionImageView() {
        transitionImageView.setVisibility(GONE);
        transitionImageView.setImageBitmap(null);
    }

    private void articleTitle(@NonNull String articleTitle) {
        articleTitleView.setText(StringUtil.fromHtml(articleTitle));
    }

    private void articleSubtitle(@Nullable String articleSubtitle) {
        articleSubtitleView.setText(articleSubtitle);
    }

    private void extract(@Nullable String extract) {
        extractView.setText(StringUtil.fromHtml(extract));
    }

    private void header(@NonNull FeaturedArticleCard card) {
        headerView.setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
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
            imageContainerView.setVisibility(GONE);
        } else {
            imageContainerView.setVisibility(VISIBLE);
            imageView.loadImage(uri);
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
