package org.wikipedia.views;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.RetainingDataSourceSupplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.wikipedia.R;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public class FaceAndColorDetectImageView extends SimpleDraweeView {
    private RetainingDataSourceSupplier<CloseableReference<CloseableImage>> supplier;

    public interface OnImageLoadListener {
        void onImageLoaded(int bmpHeight, @Nullable PointF faceLocation, @ColorInt int mainColor);
        void onImageFailed();
    }

    @NonNull private OnImageLoadListener listener = new DefaultListener();

    public FaceAndColorDetectImageView(Context context) {
        super(context);
        init();
    }

    public FaceAndColorDetectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceAndColorDetectImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setOnImageLoadListener(@Nullable OnImageLoadListener listener) {
        this.listener = listener == null ? new DefaultListener() : listener;
    }

    public void loadImage(@Nullable Uri uri) {
        if (!isImageDownloadEnabled() || uri == null) {
            setImageURI((Uri) null);
            return;
        }
        loadImage(ImageRequestBuilder.newBuilderWithSource(uri));
    }

    public void loadImage(@DrawableRes int id) {
        loadImage(ImageRequestBuilder.newBuilderWithResourceId(id));
    }

    private void loadImage(@NonNull ImageRequestBuilder builder) {
        ImageRequest imageRequest = builder.setPostprocessor(new FacePostprocessor(listener)).build();
        supplier.replaceSupplier(Fresco.getImagePipeline()
                .getDataSourceSupplier(imageRequest, null, ImageRequest.RequestLevel.FULL_FETCH));
    }

    private void init() {
        supplier = new RetainingDataSourceSupplier<>();
        setController(Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(true)
                .setDataSourceSupplier(supplier)
                .build());
    }

    private class DefaultListener implements OnImageLoadListener {
        @Override
        public void onImageLoaded(int bmpHeight, @Nullable final PointF faceLocation, @ColorInt int mainColor) {
            if (isAttachedToWindow() && faceLocation != null) {
                post(() -> {
                    if (isAttachedToWindow()) {
                        getHierarchy().setActualImageFocusPoint(faceLocation);
                    }
                });
            }
        }

        @Override
        public void onImageFailed() {
            setActualImageResource(R.drawable.lead_default);
        }
    }
}
