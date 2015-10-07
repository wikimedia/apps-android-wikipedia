package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.util.log.L;

public class ImageViewWithFace extends ImageView implements Target {
    public interface OnImageLoadListener {
        void onImageLoaded(Bitmap bitmap, @Nullable PointF faceLocation);
        void onImageFailed();
    }

    @NonNull private OnImageLoadListener listener = new DefaultListener();

    public ImageViewWithFace(Context context) {
        super(context);
    }

    public ImageViewWithFace(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewWithFace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnImageLoadListener(@Nullable OnImageLoadListener listener) {
        this.listener = listener == null ? new DefaultListener() : listener;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        if (isBitmapEligibleForFaceDetection(bitmap)) {
            spawnFaceDetectionTask(bitmap);
        } else {
            listener.onImageFailed();
        }

        // and, of course, set the original bitmap as our image
        setImageBitmap(bitmap);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        listener.onImageFailed();
    }

    @Override public void onPrepareLoad(Drawable placeHolderDrawable) { }

    private boolean isBitmapEligibleForFaceDetection(Bitmap bitmap) {
        final int minSize = 64;
        return bitmap.getWidth() >= minSize && bitmap.getHeight() >= minSize;
    }

    private void spawnFaceDetectionTask(@NonNull final Bitmap bitmap) {
        new FaceDetectionTask(SaneAsyncTask.LOW_CONCURRENCY, bitmap) {
            @Override
            public PointF performTask() {
                PointF facePos = super.performTask();
                listener.onImageLoaded(bitmap, facePos);
                return facePos;
            }

            @Override
            public void onCatch(Throwable caught) {
                // it's not super important to do anything if face detection fails,
                // but let our listener know about it anyway:
                listener.onImageFailed();
            }
        }.execute();
    }

    private static class FaceDetectionTask extends SaneAsyncTask<PointF> {
        // Width to which to reduce image copy on which face detection is performed in onBitMapLoaded()
        // (with height reduced proportionally there).  Performing face detection on a scaled-down
        // image copy improves speed and memory use.
        //
        // Also, note that the face detector requires that the image width be even.
        private static final int BITMAP_COPY_WIDTH = 200;

        @NonNull private final Bitmap srcBitmap;

        public FaceDetectionTask(int concurrency, @NonNull Bitmap bitmap) {
            super(concurrency);
            srcBitmap = bitmap;
        }

        @Override
        @Nullable
        public PointF performTask() {
            // boost this thread's priority a bit
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);
            long millis = System.currentTimeMillis();
            // create a new bitmap onto which we'll draw the original bitmap,
            // because the FaceDetector requires it to be a 565 bitmap, and it
            // must also be even width. Reduce size of copy for performance.
            Bitmap testBmp = new565ScaledBitmap(srcBitmap);

            // initialize the face detector, and look for only one face...
            FaceDetector fd = new FaceDetector(testBmp.getWidth(), testBmp.getHeight(), 1);
            FaceDetector.Face[] faces = new FaceDetector.Face[1];
            int numFound = fd.findFaces(testBmp, faces);

            PointF facePos = null;
            if (numFound > 0) {
                facePos = new PointF();
                faces[0].getMidPoint(facePos);
                // scale back to proportions of original image
                facePos.x = (facePos.x * srcBitmap.getWidth() / BITMAP_COPY_WIDTH);
                facePos.y = (facePos.y * srcBitmap.getHeight() / testBmp.getHeight());
                L.d("Found face at " + facePos.x + ", " + facePos.y);
            }
            // free our temporary bitmap
            testBmp.recycle();

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
    }

    private static class DefaultListener implements OnImageLoadListener {
        @Override public void onImageLoaded(Bitmap bitmap, PointF faceLocation) { }
        @Override public void onImageFailed() { }
    }
}