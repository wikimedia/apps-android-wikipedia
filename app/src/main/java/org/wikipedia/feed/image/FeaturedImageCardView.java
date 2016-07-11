package org.wikipedia.feed.image;

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
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedImageCardView extends FeedCardView
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    @Nullable private FeaturedImageCard card;
    @BindView(R.id.view_featured_image_card_header) View headerView;
    @BindView(R.id.view_featured_image_card_footer) View footerView;
    @BindView(R.id.view_featured_image_card_image) SimpleDraweeView imageView;
    @BindView(R.id.featured_image_description_Text) TextView descriptionView;

    public FeaturedImageCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_image, this);
        ButterKnife.bind(this);
    }

    public void set(@NonNull FeaturedImageCard card) {
        this.card = card;
        // TODO: superimpose text onto image thumb
        image(card.image());
        description(StringUtil.defaultIfNull(card.description(), ""));  //Can check language before doing this if we want
        header(card);
        footer();
        onClickListener(new CardClickListener());
    }

    private void image(@NonNull Uri uri) {
        imageView.setImageURI(uri);
    }

    private void description(@NonNull String text) {
        descriptionView.setText(RichTextUtil.stripHtml(text));
    }

    private void onClickListener(@NonNull OnClickListener listener) {
        imageView.setOnClickListener(listener);
    }

    private void header(@NonNull FeaturedImageCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.icon_potd_photo_camera)
                .setImageCircleColor(R.color.gray_highlight)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
    }

    private void footer() {
        footer(new ActionFooterView(getContext())
                .actionIcon(R.drawable.ic_file_download)
                .actionText(R.string.view_featured_image_card_download)
                .onActionListener(new CardDownloadListener())
                .onShareListener(new CardShareListener()));
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
                //getCallback().onSelectPage(card.historyEntry(HistoryEntry.SOURCE_FEED_FEATURED));
            }
        }
    }

    private class CardDownloadListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null) {
                getCallback().onDownloadImage(card.baseImage());
            }
        }
    }

    private class CardShareListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null) {
                getCallback().onShareImage(card);
            }
        }
    }
}
