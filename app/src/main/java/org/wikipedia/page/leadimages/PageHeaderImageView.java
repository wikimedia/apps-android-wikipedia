package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.wikipedia.R;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.GradientUtil.getPowerGradient;

public class PageHeaderImageView extends FrameLayout {
    @BindView(R.id.view_page_header_image_image) FaceAndColorDetectImageView image;
    @BindView(R.id.view_page_header_image_gradient) View gradientView;

    public PageHeaderImageView(Context context) {
        super(context);
        init();
    }

    public PageHeaderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageHeaderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setLoadListener(@Nullable FaceAndColorDetectImageView.OnImageLoadListener listener) {
        image.setOnImageLoadListener(listener);
    }

    public void load(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            image.loadImage(Uri.parse(url));
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

    public FaceAndColorDetectImageView getImage() {
        return image;
    }

    public void setFocusPoint(PointF focusPoint) {
        image.getHierarchy().setActualImageFocusPoint(focusPoint);
    }

    private void init() {
        setVisibility(GONE);

        // Clip the Ken Burns zoom animation applied to the image.
        setClipChildren(true);

        inflate(getContext(), R.layout.view_page_header_image, this);
        ButterKnife.bind(this);

        gradientView.setBackground(getPowerGradient(R.color.black38, Gravity.TOP));
    }
}
