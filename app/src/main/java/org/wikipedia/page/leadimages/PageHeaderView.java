package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import org.wikipedia.R;
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
    @BindView(R.id.view_page_header_image_gradient_top) View gradientViewTop;
    @BindView(R.id.view_page_header_image_gradient_bottom) View gradientViewBottom;
    @BindView(R.id.call_to_action_container) View callToActionContainer;
    @BindView(R.id.call_to_action_text) TextView callToActionTextView;
    @Nullable private Callback callback;

    public interface Callback {
        void onImageClicked();
        void onCallToActionClicked();
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

    public void show() {
        setVisibility(View.VISIBLE);
    }

    @NonNull public View getImageView() {
        return image;
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setUpCallToAction(String callToActionText) {
        if (callToActionText != null) {
            callToActionContainer.setVisibility(VISIBLE);
            callToActionTextView.setText(callToActionText);
            gradientViewBottom.setVisibility(VISIBLE);
        } else {
            callToActionContainer.setVisibility(GONE);
            gradientViewBottom.setVisibility(GONE);
        }
    }

    public void loadImage(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            hide();
        } else {
            show();
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

    @OnClick(R.id.call_to_action_container) void onCallToActionClicked() {
        if (callback != null) {
            callback.onCallToActionClicked();
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
        gradientViewTop.setBackground(getPowerGradient(R.color.black38, Gravity.TOP));
        gradientViewBottom.setBackground(getPowerGradient(R.color.black38, Gravity.BOTTOM));

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
