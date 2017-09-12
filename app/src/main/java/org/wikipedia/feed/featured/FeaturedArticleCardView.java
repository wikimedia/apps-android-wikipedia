package org.wikipedia.feed.featured;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.feed.view.ActionFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FeaturedArticleCardView extends DefaultFeedCardView<FeaturedArticleCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {

    public interface Callback {
        void onAddFeaturedPageToList(@NonNull FeaturedArticleCard card, @NonNull HistoryEntry entry);
        void onRemoveFeaturedPageFromList(@NonNull FeaturedArticleCard card, @NonNull HistoryEntry entry);
    }

    @BindView(R.id.view_featured_article_card_header) View headerView;
    @BindView(R.id.view_featured_article_card_footer) View footerView;
    @BindView(R.id.view_featured_article_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_featured_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_featured_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_featured_article_card_extract) TextView extractView;

    public FeaturedArticleCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_article, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull FeaturedArticleCard card) {
        super.setCard(card);

        String articleTitle = card.articleTitle();
        String articleSubtitle = card.articleSubtitle();
        String extract = card.extract();
        Uri imageUri = card.image();

        articleTitle(articleTitle);
        articleSubtitle(articleSubtitle);
        extract(extract);
        image(imageUri);

        header(card);
        footer(card);
    }

    @OnClick({R.id.view_featured_article_card_image, R.id.view_featured_article_card_text_container})
    void onCardClick() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onSelectPage(getCard(),
                    getCard().historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
        }
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        if (headerView instanceof CardHeaderView) {
            ((CardHeaderView) headerView).setCallback(callback);
        }
    }

    private void articleTitle(@NonNull String articleTitle) {
        articleTitleView.setText(articleTitle);
    }

    private void articleSubtitle(@Nullable String articleSubtitle) {
        articleSubtitleView.setText(articleSubtitle);
    }

    private void extract(@Nullable String extract) {
        extractView.setText(extract);
    }

    private void header(@NonNull FeaturedArticleCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_star_black_24dp)
                .setImageCircleColor(R.color.yellow50)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
    }

    private void footer(@NonNull FeaturedArticleCard card) {
        PageTitle title = new PageTitle(card.articleTitle(), card.wikiSite());
        ReadingList.DAO.anyListContainsTitleAsync(ReadingListDaoProxy.key(title),
                new CallbackTask.DefaultCallback<ReadingListPage>() {
                    @Override public void success(@Nullable ReadingListPage page) {
                        boolean listContainsTitle = page != null;

                        int actionIcon = listContainsTitle
                                ? R.drawable.ic_bookmark_white_24dp
                                : R.drawable.ic_bookmark_border_black_24dp;

                        int actionText = listContainsTitle
                                ? R.string.view_featured_article_footer_saved_button_label
                                : R.string.view_featured_article_footer_save_button_label;

                        ActionFooterView footer = new ActionFooterView(getContext())
                                .actionIcon(actionIcon)
                                .actionText(actionText)
                                .onActionListener(listContainsTitle
                                        ? new CardBookmarkMenuListener()
                                        : new CardAddToListListener())
                                .onShareListener(new CardShareListener());

                        if (listContainsTitle) {
                            footer.actionIconColor(ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent));
                            footer.actionTextColor(ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent));
                        }

                        footer(footer);
                    }
                });
    }

    private void image(@Nullable Uri uri) {
        if (uri == null) {
            imageView.setVisibility(GONE);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView.loadImage(uri);
        }
    }

    private void header(@NonNull View view) {
        ViewUtil.replace(headerView, view);
        headerView = view;
    }

    private void footer(@NonNull View view) {
        ViewUtil.replace(footerView, view);
        footerView = view;
    }

    @NonNull private HistoryEntry getEntry() {
        return getCard().historyEntry(HistoryEntry.SOURCE_FEED_FEATURED);
    }

    private class CardAddToListListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onAddFeaturedPageToList(getCard(), getEntry());
            }
        }
    }

    private class CardBookmarkMenuListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                new ReadingListBookmarkMenu(footerView, new ReadingListBookmarkMenu.Callback() {
                    @Override
                    public void onAddRequest(@Nullable ReadingListPage page) {
                        if (getCallback() != null && getCard() != null) {
                            getCallback().onAddFeaturedPageToList(getCard(),
                                    getCard().historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
                        }
                    }

                    @Override
                    public void onDeleted(@Nullable ReadingListPage page) {
                        if (getCallback() != null && getCard() != null) {
                            getCallback().onRemoveFeaturedPageFromList(getCard(), getEntry());
                        }
                    }
                }).show(getEntry().getTitle());
            }
        }
    }

    private class CardShareListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onSharePage(getEntry());
            }
        }
    }
}
