package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.wikipedia.R;
import org.wikipedia.page.leadimages.ImageViewWithFace;

public class ArticleHeaderView extends FrameLayout {
    private ImageViewWithFace image;
    private ImageView placeholder;
    private AppTextView text;

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
        findViews();
    }

    private void inflate() {
        inflate(getContext(), R.layout.view_article_header, this);
    }

    private void findViews() {
        // TODO: replace manual assignments with Butter Knife annotations.
        image = findView(R.id.image);
        placeholder = findView(R.id.placeholder);
        text = findView(R.id.text);
    }

    private <T extends View> T findView(@IdRes int id) {
        return ViewUtil.findView(this, id);
    }
}