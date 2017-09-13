package org.wikipedia.views;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public class FaceAndColorDetectImageView extends SimpleDraweeView {

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
            setImageURI((Uri) null);
            return;
        }
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setPostprocessor(new FacePostprocessor(listener))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setImageRequest(request)
                        .setAutoPlayAnimations(true)
                        .build();
        setController(controller);
    }

    private class DefaultListener implements OnImageLoadListener {
        @Override
        public void onImageLoaded(int bmpHeight, @Nullable final PointF faceLocation, @ColorInt int mainColor) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (faceLocation != null) {
                        getHierarchy().setActualImageFocusPoint(faceLocation);
                    }
                }
            });
        }

        @Override
        public void onImageFailed() {
        }
    }
}
