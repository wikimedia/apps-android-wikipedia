package org.wikipedia.feed.image;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.views.ImageZoomHelper;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class FeaturedImageCardView extends DefaultFeedCardView<FeaturedImageCard> {
    public interface Callback {
        void onShareImage(@NonNull FeaturedImageCard card);
        void onDownloadImage(@NonNull FeaturedImage image);
        void onFeaturedImageSelected(@NonNull FeaturedImageCard card);
    }

    @BindView(R.id.view_featured_image_card_content_container) View containerView;
    @BindView(R.id.view_featured_image_card_header) CardHeaderView headerView;
    @BindView(R.id.view_featured_image_card_image_placeholder) FrameLayout imageViewPlaceholder;
    @BindView(R.id.view_featured_image_card_image) ImageView imageView;
    @BindView(R.id.view_featured_image_card_image_description) TextView descriptionView;
    @BindView(R.id.view_featured_image_card_download_button) MaterialButton downloadButton;
    @BindView(R.id.view_featured_image_card_share_button) MaterialButton shareButton;

    public FeaturedImageCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_image, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull FeaturedImageCard card) {
        super.setCard(card);
        image(card.baseImage());
        description(defaultString(card.description()));  //Can check language before doing this if we want
        header();
        setClickListeners();
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    private void image(@NonNull FeaturedImage image) {
        containerView.post(() -> {
            if (!isAttachedToWindow()) {
                return;
            }
            loadImage(image);
        });
    }

    private void loadImage(@NonNull FeaturedImage image) {
        ImageZoomHelper.setViewZoomable(imageView);
        ViewUtil.loadImage(imageView, image.getThumbnailUrl());
        imageViewPlaceholder.setLayoutParams(new LayoutParams(containerView.getWidth(),
                ViewUtil.adjustImagePlaceholderHeight((float) containerView.getWidth(), (float) image.getThumbnail().getWidth(), (float) image.getThumbnail().getHeight())));
    }

    private void description(@NonNull String text) {
        descriptionView.setText(RichTextUtil.stripHtml(text));
    }

    private void setClickListeners() {
        containerView.setOnClickListener(new CardClickListener());
        downloadButton.setOnClickListener(new CardDownloadListener());
        shareButton.setOnClickListener(new CardShareListener());
    }

    private void header() {
        if (getCard() == null) {
            return;
        }
        headerView.setTitle(getCard().title())
                .setLangCode(null)
                .setCard(getCard())
                .setCallback(getCallback());
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
