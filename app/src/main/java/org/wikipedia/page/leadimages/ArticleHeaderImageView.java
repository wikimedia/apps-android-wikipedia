package org.wikipedia.page.leadimages;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.wikipedia.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ArticleHeaderImageView extends FrameLayout {
    @Bind(R.id.view_article_header_image_image) ImageViewWithFace image;

    public ArticleHeaderImageView(Context context) {
        super(context);
        init();
    }

    public ArticleHeaderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArticleHeaderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ArticleHeaderImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setLoadListener(@Nullable ImageViewWithFace.OnImageLoadListener listener) {
        image.setOnImageLoadListener(listener);
    }

    public void load(@Nullable String url) {
        if (url == null) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            image.loadImage(url);
        }
    }

    public void setAnimationPaused(boolean paused) {
        if (image.getController() != null && image.getController().getAnimatable() != null) {
            if (paused) {
                image.getController().getAnimatable().stop();
            } else {
                image.getController().getAnimatable().start();
            }
        }
    }

    public boolean hasImage() {
        return getVisibility() != GONE;
    }

    public ImageViewWithFace getImage() {
        return image;
    }

    public void setFocusOffset(float verticalOffset) {
        final float centerHorizontal = 0.5f;
        image.getHierarchy().setActualImageFocusPoint(new PointF(centerHorizontal, verticalOffset));
    }

    private void init() {
        setVisibility(GONE);

        // Clip the Ken Burns zoom animation applied to the image.
        setClipChildren(true);

        inflate();
        bind();
    }

    private void inflate() {
        inflate(getContext(), R.layout.view_article_header_image, this);
    }

    private void bind() {
        ButterKnife.bind(this);
    }
}