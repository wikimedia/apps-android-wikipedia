package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.LinearLayoutOverWebView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.util.DimenUtil.leadImageHeightForDevice;
import static org.wikipedia.util.GradientUtil.getPowerGradient;

public class PageHeaderView extends LinearLayoutOverWebView implements ObservableWebView.OnScrollChangeListener {
    @BindView(R.id.view_page_header_image) FaceAndColorDetectImageView image;
    @BindView(R.id.view_page_header_image_gradient) View gradientView;
    @Nullable private Callback callback;

    public interface Callback {
        void onImageClicked();
    }

    public PageHeaderView(Context context) {
        super(context);
        init();
    }

    public PageHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void show(boolean imageEnabled) {
        setVisibility(View.VISIBLE);
        DimenUtil.setViewHeight(this, imageEnabled ? leadImageHeightForDevice()
                : getResources().getDimensionPixelSize(R.dimen.lead_no_image_top_offset_dp));
    }

    @NonNull public View getImageView() {
        return image;
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void loadImage(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            image.setVisibility(GONE);
            gradientView.setVisibility(GONE);
        } else {
            image.setVisibility(VISIBLE);
            gradientView.setVisibility(VISIBLE);
            image.loadImage(Uri.parse(url));
        }
    }

    @NonNull
    public Bitmap copyBitmap() {
        return ViewUtil.getBitmapFromView(image);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        updateScroll(scrollY);
    }

    @OnClick(R.id.view_page_header_image) void onImageClick() {
        if (callback != null) {
            callback.onImageClicked();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateScroll();
    }

    private void updateScroll() {
        updateScroll((int) -getTranslationY());
    }

    private void updateScroll(int scrollY) {
        int offset = Math.min(getHeight(), scrollY);
        image.setTranslationY(offset / 2);
        setTranslationY(-offset);
    }

    private void init() {
        inflate(getContext(), R.layout.view_page_header, this);
        ButterKnife.bind(this);
        ViewCompat.setTransitionName(this, getContext().getString(R.string.transition_floating_queue));
        gradientView.setBackground(getPowerGradient(R.color.black38, Gravity.TOP));

        image.setOnImageLoadListener(new ImageLoadListener());
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, leadImageHeightForDevice()));
    }

    private class ImageLoadListener implements FaceAndColorDetectImageView.OnImageLoadListener {
        @Override
        public void onImageLoaded(final int bmpHeight, @Nullable final PointF faceLocation, @ColorInt final int mainColor) {
            if (isAttachedToWindow() && faceLocation != null) {
                image.post(() -> {
                    if (isAttachedToWindow()) {
                        image.getHierarchy().setActualImageFocusPoint(faceLocation);
                        updateScroll();
                    }
                });
            }
        }

        @Override
        public void onImageFailed() {
        }
    }
}
