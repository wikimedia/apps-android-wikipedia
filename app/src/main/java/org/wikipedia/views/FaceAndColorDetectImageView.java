package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.WhiteBackgroundTransformation;
import org.wikipedia.util.log.L;

import java.security.MessageDigest;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public class FaceAndColorDetectImageView extends AppCompatImageView {
    public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
    private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
    private static final int BITMAP_COPY_WIDTH = 200;
    private static final CenterCropWithFace FACE_DETECT_TRANSFORM = new CenterCropWithFace();
    private static final MultiTransformation<Bitmap> FACE_DETECT_TRANSFORM_AND_ROUNDED_CORNERS = new MultiTransformation<>(FACE_DETECT_TRANSFORM, ViewUtil.getRoundedCorners());
    private static final MultiTransformation<Bitmap> CENTER_CROP_WHITE_BACKGROUND = new MultiTransformation<>(new CenterCrop(), new WhiteBackgroundTransformation());
    private static final Paint PAINT_WHITE = new Paint();
    private static final Paint PAINT_DARK_OVERLAY = new Paint();

    static {
        final int blackAlpha = 100;
        PAINT_WHITE.setColor(Color.WHITE);
        PAINT_DARK_OVERLAY.setColor(Color.argb(blackAlpha, 0, 0, 0));
    }

    public interface OnImageLoadListener {
        void onImageLoaded(@NonNull Palette palette);
        void onImageFailed();
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
        loadImage(uri, false, null);
    }

    public void loadImage(@Nullable Uri uri, boolean roundedCorners, @Nullable OnImageLoadListener listener) {
        Drawable placeholder = ViewUtil.getPlaceholderDrawable(getContext());
        if (!isImageDownloadEnabled() || uri == null) {
            setImageDrawable(placeholder);
            return;
        }
        RequestBuilder<Drawable> builder = Glide.with(this)
                .load(uri)
                .placeholder(placeholder)
                .error(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE);

        if (listener != null) {
            builder = builder.listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    listener.onImageFailed();
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    if (resource instanceof BitmapDrawable && ((BitmapDrawable) resource).getBitmap() != null) {
                        listener.onImageLoaded(Palette.from(((BitmapDrawable) resource).getBitmap()).generate());
                    } else {
                        listener.onImageFailed();
                    }
                    return false;
                }
            });
        }

        if (shouldDetectFace(uri)) {
            builder = builder.transform(roundedCorners ? FACE_DETECT_TRANSFORM_AND_ROUNDED_CORNERS : FACE_DETECT_TRANSFORM);
        } else {
            builder = builder.transform(roundedCorners ? ViewUtil.getCenterCropLargeRoundedCorners() : CENTER_CROP_WHITE_BACKGROUND);
        }

        builder.into(this);
    }

    public void loadImage(@DrawableRes int id) {
        this.setImageResource(id);
    }

    private static boolean shouldDetectFace(@NonNull Uri uri) {
        // TODO: not perfect; should ideally detect based on MIME type.
        String path = StringUtils.defaultString(uri.getPath()).toLowerCase();
        return path.endsWith(".jpg") || path.endsWith(".jpeg");
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

            if (isBitmapEligibleForImageProcessing(inBitmap)) {
                Bitmap testBmp = new565ScaledBitmap(pool, inBitmap);
                try {
                    facePos = detectFace(testBmp);
                } catch (OutOfMemoryError e) {
                    L.logRemoteErrorIfProd(e);
                }
                pool.put(testBmp);
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
        return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.RGB_565;
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

    private static boolean isBitmapEligibleForImageProcessing(@NonNull Bitmap bitmap) {
        final int minSize = 64;
        return bitmap.getWidth() >= minSize && bitmap.getHeight() >= minSize;
    }
}
