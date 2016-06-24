package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedCardFooterView extends FrameLayout {
    @BindView(R.id.view_card_featured_footer_save_button) View saveButton;
    @BindView(R.id.view_card_featured_footer_share_button) View shareButton;

    public FeaturedCardFooterView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_footer, this);
        ButterKnife.bind(this);
    }

    public FeaturedCardFooterView onSaveListener(@Nullable OnClickListener listener) {
        saveButton.setOnClickListener(listener);
        return this;
    }

    public FeaturedCardFooterView onShareListener(@Nullable OnClickListener listener) {
        shareButton.setOnClickListener(listener);
        return this;
    }
}
