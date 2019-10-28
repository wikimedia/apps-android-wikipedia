package org.wikipedia.views;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public class FaceAndColorDetectImageView extends AppCompatImageView {

    public interface OnImageLoadListener {
        void onImageLoaded(int bmpHeight, @Nullable PointF faceLocation, @ColorInt int mainColor);
        void onImageFailed();
    }

    @NonNull private OnImageLoadListener listener = new DefaultListener();

    public FaceAndColorDetectImageView(Context context) {
        super(context);
    }

    public FaceAndColorDetectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FaceAndColorDetectImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnImageLoadListener(@Nullable OnImageLoadListener listener) {
        this.listener = listener == null ? new DefaultListener() : listener;
    }

    public void loadImage(@Nullable Uri uri) {
        if (!isImageDownloadEnabled() || uri == null) {
            setImageURI(null);
            return;
        }
        Glide.with(this).load(uri).into(this);
    }

    public void loadImage(@DrawableRes int id) {
        this.setImageResource(id);
    }


    private class DefaultListener implements OnImageLoadListener {
        @Override
        public void onImageLoaded(int bmpHeight, @Nullable final PointF faceLocation, @ColorInt int mainColor) {
            if (isAttachedToWindow() && faceLocation != null) {
                post(() -> {
                    if (isAttachedToWindow()) {
                        // TODO:
                        //getHierarchy().setActualImageFocusPoint(faceLocation);
                    }
                });
            }
        }

        @Override
        public void onImageFailed() {
            // TODO:
            //setActualImageResource(R.drawable.lead_default);
        }
    }
}
