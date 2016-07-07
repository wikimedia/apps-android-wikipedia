package org.wikipedia.feed.image;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedImageCardFooterView extends FrameLayout {
    @BindView(R.id.view_card_featured_image_footer_download_button) View downloadButton;
    @BindView(R.id.view_card_featured_image_footer_share_button) View shareButton;

    public FeaturedImageCardFooterView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_image_footer, this);
        ButterKnife.bind(this);
    }

    public FeaturedImageCardFooterView onDownloadListener(@Nullable OnClickListener listener) {
        downloadButton.setOnClickListener(listener);
        return this;
    }

    public FeaturedImageCardFooterView onShareListener(@Nullable OnClickListener listener) {
        shareButton.setOnClickListener(listener);
        return this;
    }
}