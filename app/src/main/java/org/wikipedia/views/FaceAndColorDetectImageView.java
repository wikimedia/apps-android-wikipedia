package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.log.L;

public class FaceAndColorDetectImageView extends SimpleDraweeView {
    private static final int BITMAP_COPY_WIDTH = 200;

    public interface OnImageLoadListener {
        void onImageLoaded(int bmpHeight, @Nullable PointF faceLocation, @ColorInt int mainColor);
        void onImageFailed();
    }

    private FacePostprocessor facePostprocessor = new FacePostprocessor();
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
        if (!WikipediaApp.getInstance().isImageDownloadEnabled() || uri == null) {
            setImageURI(null);
            return;
        }
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setProgressiveRenderingEnabled(true)
                .setPostprocessor(facePostprocessor)
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setImageRequest(request)
                        .setAutoPlayAnimations(true)
                        .build();
        setController(controller);
    }

    @Nullable private PointF detectFace(Bitmap testBitmap) {
        final int maxFaces = 1;
        long millis = System.currentTimeMillis();
        // initialize the face detector, and look for only one face...
        FaceDetector fd = new FaceDetector(testBitmap.getWidth(), testBitmap.getHeight(), maxFaces);
        FaceDetector.Face[] faces = new FaceDetector.Face[maxFaces];
        int numFound = fd.findFaces(testBitmap, faces);
        PointF facePos = null;
        if (numFound > 0) {
            facePos = new PointF();
            faces[0].getMidPoint(facePos);
            // center on the nose, not on the eyes
            facePos.y += faces[0].eyesDistance() / 2;
            // normalize the position to [0, 1]
            facePos.set(MathUtil.constrain(facePos.x / testBitmap.getWidth(), 0, 1),
                    MathUtil.constrain(facePos.y / testBitmap.getHeight(), 0, 1));
            L.d("Found face at " + facePos.x + ", " + facePos.y);
        }
        L.d("Face detection took " + (System.currentTimeMillis() - millis) + "ms");
        return facePos;
    }

    @NonNull private Bitmap new565ScaledBitmap(Bitmap src) {
        Bitmap copy =  Bitmap.createBitmap(BITMAP_COPY_WIDTH,
                (src.getHeight() * BITMAP_COPY_WIDTH) / src.getWidth(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(copy);
        Rect srcRect = new Rect(0, 0, src.getWidth(), src.getHeight());
        Rect destRect = new Rect(0, 0, BITMAP_COPY_WIDTH, copy.getHeight());
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(src, srcRect, destRect, paint);
        return copy;
    }

    @ColorInt private int extractMainColor(Palette colorPalette, @ColorInt int defaultColor) {
        int mainColor = defaultColor;
        if (colorPalette.getDarkMutedSwatch() != null) {
            mainColor = colorPalette.getDarkMutedSwatch().getRgb();
        } else if (colorPalette.getDarkVibrantSwatch() != null) {
            mainColor = colorPalette.getDarkVibrantSwatch().getRgb();
        }
        return mainColor;
    }

    private boolean isBitmapEligibleForImageProcessing(Bitmap bitmap) {
        final int minSize = 64;
        return bitmap.getWidth() >= minSize && bitmap.getHeight() >= minSize;
    }

    private class FacePostprocessor extends BasePostprocessor {
        @Override
        public String getName() {
            return "FacePostprocessor";
        }

        @Override
        public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
            if (isBitmapEligibleForImageProcessing(sourceBitmap)) {
                copyWithWhiteBackground(destBitmap, sourceBitmap);
                Bitmap testBmp = new565ScaledBitmap(sourceBitmap);
                Palette colorPalette = Palette.from(testBmp).generate();
                PointF facePos = detectFace(testBmp);
                int defaultColor = getContext().getResources().getColor(R.color.dark_gray);
                listener.onImageLoaded(destBitmap.getHeight(), facePos, extractMainColor(colorPalette, defaultColor));
            } else {
                listener.onImageFailed();
            }
        }

        private void copyWithWhiteBackground(Bitmap destBitmap, Bitmap sourceBitmap) {
            Canvas canvas = new Canvas(destBitmap);
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.WHITE);
            canvas.drawRect(0f, 0f, destBitmap.getWidth(), destBitmap.getHeight(), backgroundPaint);
            canvas.drawBitmap(sourceBitmap, 0f, 0f, backgroundPaint);
        }
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