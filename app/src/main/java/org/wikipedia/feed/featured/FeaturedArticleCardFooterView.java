package org.wikipedia.feed.featured;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedArticleCardFooterView extends FrameLayout {
    @BindView(R.id.view_card_featured_article_footer_save_button) View saveButton;
    @BindView(R.id.view_card_featured_article_footer_share_button) View shareButton;

    public FeaturedArticleCardFooterView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_article_footer, this);
        ButterKnife.bind(this);
    }

    public FeaturedArticleCardFooterView onSaveListener(@Nullable OnClickListener listener) {
        saveButton.setOnClickListener(listener);
        return this;
    }

    public FeaturedArticleCardFooterView onShareListener(@Nullable OnClickListener listener) {
        shareButton.setOnClickListener(listener);
        return this;
    }
}
