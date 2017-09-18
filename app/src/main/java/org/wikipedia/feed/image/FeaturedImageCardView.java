package org.wikipedia.feed.image;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.view.ActionFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class FeaturedImageCardView extends DefaultFeedCardView<FeaturedImageCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onShareImage(@NonNull FeaturedImageCard card);
        void onDownloadImage(@NonNull FeaturedImage image);
        void onFeaturedImageSelected(@NonNull FeaturedImageCard card);
    }

    @BindView(R.id.view_featured_image_card_header) View headerView;
    @BindView(R.id.view_featured_image_card_footer) View footerView;
    @BindView(R.id.view_featured_image_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.featured_image_description_Text) TextView descriptionView;

    public FeaturedImageCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_image, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull FeaturedImageCard card) {
        super.setCard(card);
        // TODO: superimpose text onto image thumb
        image(card.image());
        description(defaultString(card.description()));  //Can check language before doing this if we want
        header(card);
        footer();
        onClickListener(new CardClickListener());
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        if (headerView instanceof CardHeaderView) {
            ((CardHeaderView) headerView).setCallback(callback);
        }
    }

    private void image(@NonNull Uri uri) {
        imageView.loadImage(uri);
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
                .setImageCircleColor(R.color.base30)
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
            if (getCallback() != null && getCard() != null) {
                getCallback().onFeaturedImageSelected(getCard());
            }
        }
    }

    private class CardDownloadListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onDownloadImage(getCard().baseImage());
            }
        }
    }

    private class CardShareListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onShareImage(getCard());
            }
        }
    }
}
