package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BigPictureCardView extends CardView {
    @BindView(R.id.view_big_picture_card_header) View headerView;
    @BindView(R.id.view_big_picture_card_footer) View footerView;
    @BindView(R.id.view_big_picture_card_image) SimpleDraweeView imageView;
    @BindView(R.id.view_big_picture_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_big_picture_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_big_picture_card_extract) TextView extractView;
    @BindView(R.id.view_big_picture_card_text_container) View textContainerView;

    public BigPictureCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_big_picture_card, this);
        ButterKnife.bind(this);
    }

    protected void onClickListener(@Nullable OnClickListener listener) {
        textContainerView.setOnClickListener(listener);
    }

    protected void articleTitle(@NonNull String articleTitle) {
        articleTitleView.setText(articleTitle);
    }

    protected void articleSubtitle(@Nullable String articleSubtitle) {
        articleSubtitleView.setText(articleSubtitle);
    }

    protected void image(@Nullable Uri uri) {
        if (uri == null) {
            imageView.setVisibility(GONE);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView.setImageURI(uri);
        }
    }

    protected void extract(@Nullable String extract) {
        extractView.setText(extract);
    }

    protected void header(@NonNull View view) {
        ViewUtil.replace(headerView, view);
        headerView = view;
    }

    protected void footer(@NonNull View view) {
        ViewUtil.replace(footerView, view);
        footerView = view;
    }
}
