package org.wikipedia.page.leadimages;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.wikipedia.R;
import org.wikipedia.ViewAnimations;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ArticleHeaderImageView extends FrameLayout {
    @Bind(R.id.view_article_header_image_placeholder) ImageView placeholder;
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
            image.setVisibility(INVISIBLE);
            placeholder.setVisibility(VISIBLE);
            Picasso.with(getContext())
                    .load(url)
                    .noFade()
                    .into((Target) image);
        }
    }

    public void crossFade() {
        ViewAnimations.crossFade(placeholder, image);
    }

    public boolean hasImage() {
        return getVisibility() != GONE;
    }

    public ImageView getImage() {
        return image;
    }

    public void setParallax(float imageYScalar, int yOffset) {
        if (image.getDrawable() != null) {
            updateImageViewParallax(image, imageYScalar, yOffset);
        }
        updateImageViewParallax(placeholder, 0, yOffset);
    }

    private void updateImageViewParallax(ImageView view, float scalar, int offset) {
        Matrix matrix = centerCropWithOffsetScalar(view, view.getDrawable(), view.getImageMatrix(), scalar);
        matrix.postTranslate(0, offset);
        view.setImageMatrix(matrix);
    }

    // See ImageView
    private Matrix centerCropWithOffsetScalar(@NonNull View view,
                                              @NonNull Drawable drawable,
                                              @NonNull Matrix initial,
                                              float offsetScalar) {
        final float halfScalar = .5f;

        Matrix matrix = new Matrix(initial);

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        int canvasWidth = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        int canvasHeight = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();

        float scale;
        float dx = 0;
        float dy = 0;
        if (drawableWidth * canvasHeight > canvasWidth * drawableHeight) {
            scale = (float) canvasHeight / (float) drawableHeight;
            dx = (canvasWidth - drawableWidth * scale) * halfScalar;
        } else {
            scale = (float) canvasWidth / (float) drawableWidth;
            dy = (canvasHeight - drawableHeight * scale) * halfScalar;
        }

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);

        float y = (canvasHeight - drawableHeight * scale) * (offsetScalar - halfScalar);
        matrix.postTranslate(0, y);

        return matrix;
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