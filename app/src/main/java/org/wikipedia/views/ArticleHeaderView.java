package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.wikipedia.R;
import org.wikipedia.page.leadimages.ImageViewWithFace;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ArticleHeaderView extends FrameLayout {
    @Bind(R.id.image) ImageViewWithFace image;
    @Bind(R.id.placeholder) ImageView placeholder;
    @Bind(R.id.text) AppTextView text;

    public ArticleHeaderView(Context context) {
        super(context);
        init();
    }

    public ArticleHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArticleHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ArticleHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ImageViewWithFace getImage() {
        return image;
    }

    public ImageView getPlaceholder() {
        return placeholder;
    }

    public AppTextView getText() {
        return text;
    }

    private void init() {
        inflate();
        ButterKnife.bind(this);
    }

    private void inflate() {
        inflate(getContext(), R.layout.view_article_header, this);
    }
}