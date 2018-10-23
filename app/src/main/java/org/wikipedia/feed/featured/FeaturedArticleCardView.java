package org.wikipedia.feed.featured;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.ArticleSavedOrDeletedEvent;
import org.wikipedia.feed.view.ActionFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class FeaturedArticleCardView extends DefaultFeedCardView<FeaturedArticleCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {

    @BindView(R.id.view_featured_article_card_header) CardHeaderView headerView;
    @BindView(R.id.view_featured_article_card_footer) ActionFooterView footerView;
    @BindView(R.id.view_featured_article_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_featured_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_featured_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_featured_article_card_extract) TextView extractView;
    @BindView(R.id.view_featured_article_card_text_container) View textContainerView;
    private CompositeDisposable disposables = new CompositeDisposable();

    public FeaturedArticleCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_article, this);
        ButterKnife.bind(this);
    }

    public void setCard(@NonNull FeaturedArticleCard card) {
        super.setCard(card);
        setLayoutDirectionByWikiSite(card.wikiSite(), textContainerView);

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.clear();
        super.onDetachedFromWindow();
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
        headerView.setCallback(callback);
    }

    private void articleTitle(@NonNull String articleTitle) {
        articleTitleView.setText(articleTitle);
    }

    private void articleSubtitle(@Nullable String articleSubtitle) {
        articleSubtitleView.setText(articleSubtitle);
    }

    private void extract(@Nullable String extract) {
        extractView.setText(Html.fromHtml(extract));
    }

    private void header(@NonNull FeaturedArticleCard card) {
        headerView.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_star_black_24dp)
                .setImageCircleColor(R.color.yellow50)
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());
    }

    @SuppressLint("CheckResult")
    private void footer(@NonNull FeaturedArticleCard card) {
        PageTitle title = new PageTitle(card.articleTitle(), card.wikiSite());
        Observable.fromCallable(() -> ReadingListDbHelper.instance().findPageInAnyList(title) != null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageInList -> {
                    int actionIcon = pageInList
                            ? R.drawable.ic_bookmark_white_24dp
                            : R.drawable.ic_bookmark_border_black_24dp;

                    int actionText = pageInList
                            ? R.string.view_featured_article_footer_saved_button_label
                            : R.string.view_featured_article_footer_save_button_label;

                    footerView.actionIcon(actionIcon)
                            .actionText(actionText)
                            .onActionListener(pageInList
                                    ? new CardBookmarkMenuListener()
                                    : new CardAddToListListener())
                            .onShareListener(new CardShareListener());

                    footerView.actionIconColor(ResourceUtil.getThemedAttributeId(getContext(),
                            pageInList ? R.attr.colorAccent : R.attr.secondary_text_color));
                    footerView.actionTextColor(ResourceUtil.getThemedAttributeId(getContext(),
                            pageInList ? R.attr.colorAccent : R.attr.secondary_text_color));
                }, L::w);
    }

    private void image(@Nullable Uri uri) {
        if (uri == null) {
            imageView.setVisibility(GONE);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView.loadImage(uri);
        }
    }

    @NonNull private HistoryEntry getEntry() {
        return getCard().historyEntry(HistoryEntry.SOURCE_FEED_FEATURED);
    }

    private class CardAddToListListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onAddPageToList(getEntry());
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
                            getCallback().onAddPageToList(getCard().historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
                        }
                    }

                    @Override
                    public void onDeleted(@Nullable ReadingListPage page) {
                        if (getCallback() != null && getCard() != null) {
                            getCallback().onRemovePageFromList(getEntry());
                        }
                    }

                    @Override
                    public void onShare() {
                        // ignore
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

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ArticleSavedOrDeletedEvent) {
                if (getCard() == null) {
                    return;
                }
                for (ReadingListPage page : ((ArticleSavedOrDeletedEvent) event).getPages()) {
                    if (page.title().equals(getCard().articleTitle())) {
                        footer(getCard());
                    }
                }
            }
        }
    }
}
