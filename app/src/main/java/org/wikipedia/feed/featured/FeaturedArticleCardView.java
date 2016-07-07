package org.wikipedia.feed.featured;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.feed.view.ActionFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.FeedCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedArticleCardView extends FeedCardView
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    @BindView(R.id.view_featured_article_card_header) View headerView;
    @BindView(R.id.view_featured_article_card_footer) View footerView;
    @BindView(R.id.view_featured_article_card_image) SimpleDraweeView imageView;
    @BindView(R.id.view_featured_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_featured_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_featured_article_card_extract) TextView extractView;
    @BindView(R.id.view_featured_article_card_text_container) View textContainerView;

    private FeaturedArticleCard card;

    public FeaturedArticleCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_article, this);
        ButterKnife.bind(this);
    }

    public void set(@NonNull FeaturedArticleCard card) {
        this.card = card;
        String articleTitle = card.articleTitle();
        String articleSubtitle = card.articleSubtitle();
        String extract = card.extract();
        Uri imageUri = card.image();

        articleTitle(articleTitle);
        articleSubtitle(articleSubtitle);
        extract(extract);
        image(imageUri);

        header(card);
        footer();

        onClickListener(new CardClickListener());
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

    private void onClickListener(@Nullable OnClickListener listener) {
        textContainerView.setOnClickListener(listener);
    }

    private void header(@NonNull FeaturedArticleCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_star_black_24dp)
                .setImageCircleColor(R.color.feed_featured_icon_background)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
    }

    private void footer() {
        footer(new ActionFooterView(getContext())
                .actionIcon(R.drawable.ic_bookmark_border_black_24dp)
                .actionText(R.string.view_featured_article_footer_save_button_label)
                .onActionListener(new CardSaveListener())
                .onShareListener(new CardShareListener()));
    }

    private void image(@Nullable Uri uri) {
        if (uri == null) {
            imageView.setVisibility(GONE);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView.setImageURI(uri);
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

    private class CardClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null) {
                getCallback().onSelectPage(card.historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
            }
        }
    }

    private class CardSaveListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null) {
                getCallback().onAddPageToList(card.historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
            }
        }
    }

    private class CardShareListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null) {
                getCallback().onSharePage(card.historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
            }
        }
    }
}
