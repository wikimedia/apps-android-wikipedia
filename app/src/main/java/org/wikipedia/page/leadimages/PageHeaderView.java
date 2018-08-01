package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

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

public class PageHeaderView extends LinearLayoutOverWebView implements ObservableWebView.OnScrollChangeListener {
    @BindView(R.id.view_page_header_image) PageHeaderImageView image;
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

    public void showText() {
        setVisibility(View.VISIBLE);
        setTopOffset();
    }

    public void showTextImage() {
        setVisibility(View.VISIBLE);
        unsetTopOffset();
        DimenUtil.setViewHeight(image, leadImageHeightForDevice());
    }

    // TODO: remove.
    @NonNull public ImageView getImage() {
        return image.getImage();
    }

    public void setOnImageLoadListener(@Nullable FaceAndColorDetectImageView.OnImageLoadListener listener) {
        image.setLoadListener(listener);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void loadImage(@Nullable String url) {
        image.load(url);
        int height = url == null ? 0 : leadImageHeightForDevice();
        setMinimumHeight(height);
    }

    public void setAnimationPaused(boolean paused) {
        image.setAnimationPaused(paused);
    }

    @NonNull
    public Bitmap copyBitmap() {
        return ViewUtil.getBitmapFromView(image.getImage());
    }

    public void setImageFocus(PointF focusPoint) {
        image.setFocusPoint(focusPoint);
        updateScroll();
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
        image.getImage().setTranslationY(offset / 2);
        setTranslationY(-offset);
    }

    private void init() {
        inflate(getContext(), R.layout.view_page_header, this);
        setOrientation(VERTICAL);
        ButterKnife.bind(this);
        hide();
    }

    private int getDimensionPixelSize(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    private void setTopOffset() {
        setTopOffset(true);
    }

    private void unsetTopOffset() {
        setTopOffset(false);
    }

    private void setTopOffset(boolean noImage) {
        int offset = noImage ? getDimensionPixelSize(R.dimen.lead_no_image_top_offset_dp) : 0;

        // Offset is a resolved pixel dimension, not a resource id
        //noinspection ResourceType
        setPadding(0, offset, 0, 0);
    }
}
