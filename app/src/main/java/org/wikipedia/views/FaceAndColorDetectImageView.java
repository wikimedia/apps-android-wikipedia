package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.log.L;

import java.security.MessageDigest;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public class FaceAndColorDetectImageView extends AppCompatImageView {
    public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
    private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
    private static final int BITMAP_COPY_WIDTH = 200;
    private static final CenterCropWithFace FACE_DETECT_TRANSFORM = new CenterCropWithFace();
    private static final Paint PAINT_WHITE = new Paint();
    private static final Paint PAINT_DARK_OVERLAY = new Paint();

    static {
        final int blackAlpha = 100;
        PAINT_WHITE.setColor(Color.WHITE);
        PAINT_DARK_OVERLAY.setColor(Color.argb(blackAlpha, 0, 0, 0));
    }

    public FaceAndColorDetectImageView(Context context) {
        super(context);
    }

    public FaceAndColorDetectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FaceAndColorDetectImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void loadImage(@Nullable Uri uri) {
        Drawable placeholder = ViewUtil.getPlaceholderDrawable(getContext());
        if (!isImageDownloadEnabled() || uri == null) {
            setImageDrawable(placeholder);
            return;
        }
        Glide.with(this)
                .load(uri)
                .placeholder(placeholder)
                .error(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .transform(FACE_DETECT_TRANSFORM)
                .into(this);
    }

    public void loadImage(@DrawableRes int id) {
        this.setImageResource(id);
    }

    private static class CenterCropWithFace extends BitmapTransformation {
        private static final String ID = "org.wikipedia.views.CenterCropWithFace";
        private final byte[] idBytes = ID.getBytes(CHARSET);

        @Override
        public boolean equals(Object o) {
            return o instanceof CenterCropWithFace;
        }

        @Override
        public int hashCode() {
            return ID.hashCode();
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            messageDigest.update(idBytes);
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width, int height) {
            if (inBitmap.getWidth() == width && inBitmap.getHeight() == height) {
                return inBitmap;
            }

            PointF facePos = null;
            @ColorInt int mainColor = ContextCompat.getColor(WikipediaApp.getInstance(), R.color.base30);

            if (isBitmapEligibleForImageProcessing(inBitmap)) {
                Bitmap testBmp = new565ScaledBitmap(pool, inBitmap);
                Palette colorPalette = Palette.from(testBmp).generate();
                try {
                    facePos = detectFace(testBmp);
                } catch (OutOfMemoryError e) {
                    L.logRemoteErrorIfProd(e);
                }
                pool.put(testBmp);
                mainColor = extractMainColor(colorPalette, mainColor);
            }

            float scale;
            float dx;
            float dy;
            final float half = 0.5f;
            Matrix m = new Matrix();
            if (inBitmap.getWidth() * height > width * inBitmap.getHeight()) {
                scale = (float) height / (float) inBitmap.getHeight();
                dx = (width - inBitmap.getWidth() * scale) * half;
                dy = 0;
            } else {
                scale = (float) width / (float) inBitmap.getWidth();
                dx = 0;

                // apply face offset if we have one
                if (facePos != null) {
                    dy = (height * half) - (inBitmap.getHeight() * scale * facePos.y);
                    if (dy > 0) {
                        dy = 0f;
                    } else if (dy < -(inBitmap.getHeight() * scale - height)) {
                        dy = -(inBitmap.getHeight() * scale - height);
                    }
                } else {
                    dy = (height - inBitmap.getHeight() * scale) * half;
                }
            }

            m.setScale(scale, scale);
            m.postTranslate((int) (dx + half), (int) (dy + half));

            Bitmap result = pool.getDirty(width, height, getNonNullConfig(inBitmap));
            // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
            TransformationUtils.setAlpha(inBitmap, result);

            applyMatrixWithBackground(inBitmap, result, m);
            return result;
        }
    }

    private static Bitmap.Config getNonNullConfig(@NonNull Bitmap bitmap) {
        return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
    }

    public static void applyMatrixWithBackground(@NonNull Bitmap inBitmap, @NonNull Bitmap targetBitmap, @NonNull Matrix matrix) {
        TransformationUtils.getBitmapDrawableLock().lock();
        try {
            Canvas canvas = new Canvas(targetBitmap);
            canvas.drawRect(0f, 0f, targetBitmap.getWidth(), targetBitmap.getHeight(), PAINT_WHITE);
            canvas.drawBitmap(inBitmap, matrix, DEFAULT_PAINT);
            if (WikipediaApp.getInstance().getCurrentTheme().isDark() && Prefs.shouldDimDarkModeImages()) {
                // "dim" images by drawing a translucent black rectangle over them.
                canvas.drawRect(0f, 0f, targetBitmap.getWidth(), targetBitmap.getHeight(), PAINT_DARK_OVERLAY);
            }
        } finally {
            TransformationUtils.getBitmapDrawableLock().unlock();
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

    @NonNull private static Bitmap new565ScaledBitmap(@NonNull BitmapPool pool, @NonNull Bitmap src) {
        Bitmap copy = pool.getDirty(BITMAP_COPY_WIDTH,
                (src.getHeight() * BITMAP_COPY_WIDTH) / src.getWidth(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(copy);
        Rect srcRect = new Rect(0, 0, src.getWidth(), src.getHeight());
        Rect destRect = new Rect(0, 0, BITMAP_COPY_WIDTH, copy.getHeight());
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(src, srcRect, destRect, paint);
        return copy;
    }

    @ColorInt private static int extractMainColor(@NonNull Palette colorPalette, @ColorInt int defaultColor) {
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
}
