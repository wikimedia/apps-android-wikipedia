package org.wikipedia.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;

import com.facebook.imagepipeline.request.BasePostprocessor;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.log.L;

import java.lang.ref.WeakReference;

public class FacePostprocessor extends BasePostprocessor {
    private static final int BITMAP_COPY_WIDTH = 200;

    @NonNull
    private WeakReference<FaceAndColorDetectImageView.OnImageLoadListener> listener;

    public FacePostprocessor(@NonNull FaceAndColorDetectImageView.OnImageLoadListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    @Override public String getName() {
        return "FacePostprocessor";
    }

    @Override public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
        if (listener.get() == null) {
            return;
        }
        if (isBitmapEligibleForImageProcessing(sourceBitmap)) {
            copyWithWhiteBackground(destBitmap, sourceBitmap);
            Bitmap testBmp = new565ScaledBitmap(sourceBitmap);
            Palette colorPalette = Palette.from(testBmp).generate();
            PointF facePos = null;
            try {
                facePos = detectFace(testBmp);
            } catch (OutOfMemoryError e) {
                L.logRemoteErrorIfProd(e);
            }
            int defaultColor = ContextCompat.getColor(WikipediaApp.getInstance(), R.color.base30);
            listener.get().onImageLoaded(destBitmap.getHeight(), facePos,
                    extractMainColor(colorPalette, defaultColor));
        } else {
            listener.get().onImageFailed();
        }
    }

    @Nullable private static PointF detectFace(@NonNull Bitmap testBitmap) {
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

    @NonNull private static Bitmap new565ScaledBitmap(@NonNull Bitmap src) {
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

    @ColorInt private static int extractMainColor(@NonNull Palette colorPalette,
                                        @ColorInt int defaultColor) {
        int mainColor = defaultColor;
        if (colorPalette.getDarkMutedSwatch() != null) {
            mainColor = colorPalette.getDarkMutedSwatch().getRgb();
        } else if (colorPalette.getDarkVibrantSwatch() != null) {
            mainColor = colorPalette.getDarkVibrantSwatch().getRgb();
        }
        return mainColor;
    }

    private static boolean isBitmapEligibleForImageProcessing(@NonNull Bitmap bitmap) {
        final int minSize = 64;
        return bitmap.getWidth() >= minSize && bitmap.getHeight() >= minSize;
    }

    private static void copyWithWhiteBackground(@NonNull Bitmap destBitmap,
                                                @NonNull Bitmap sourceBitmap) {
        Canvas canvas = new Canvas(destBitmap);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        canvas.drawRect(0f, 0f, destBitmap.getWidth(), destBitmap.getHeight(), backgroundPaint);
        canvas.drawBitmap(sourceBitmap, 0f, 0f, backgroundPaint);
        if (WikipediaApp.getInstance().getCurrentTheme().isDark() && Prefs.shouldDimDarkModeImages()) {
            // "dim" images by drawing a translucent black rectangle over them.
            final int blackAlpha = 100;
            backgroundPaint.setColor(Color.argb(blackAlpha, 0, 0, 0));
            canvas.drawRect(0f, 0f, destBitmap.getWidth(), destBitmap.getHeight(), backgroundPaint);
        }
    }
}
