package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.wikipedia.concurrency.SaneAsyncTask;

public class ImageViewWithFace extends ImageView implements Target {
    private static final String TAG = "ImageViewWithFace";

    public ImageViewWithFace(Context context) {
        super(context);
    }

    public ImageViewWithFace(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewWithFace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
        (new SaneAsyncTask<Void>(SaneAsyncTask.LOW_CONCURRENCY) {
            @Override
            public Void performTask() throws Throwable {
                final int minSize = 64;
                // don't load the image if it's terrible resolution
                // (or indeed, if it's zero width or height)
                if (bitmap.getWidth() < minSize || bitmap.getHeight() < minSize) {
                    if (onImageLoadListener != null) {
                        onImageLoadListener.onImageFailed();
                    }
                    return null;
                }
                // boost this thread's priority a bit
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);
                long millis = System.currentTimeMillis();
                // create a new bitmap onto which we'll draw the original bitmap,
                // because the FaceDetector requires it to be a 565 bitmap, and it
                // must also be even width.
                Bitmap tempbmp = Bitmap.createBitmap(bitmap.getWidth() - (bitmap.getWidth() % 2),
                        bitmap.getHeight(), Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(tempbmp);
                Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                canvas.drawBitmap(bitmap, 0, 0, paint);

                // initialize the face detector, and look for only one face...
                FaceDetector fd = new FaceDetector(tempbmp.getWidth(), tempbmp.getHeight(), 1);
                FaceDetector.Face[] faces = new FaceDetector.Face[1];
                PointF facePos = new PointF();
                int numFound = fd.findFaces(tempbmp, faces);

                if (numFound > 0) {
                    faces[0].getMidPoint(facePos);
                    android.util.Log.d(TAG, "Found face at " + facePos.x + ", " + facePos.y);
                }
                // free our temporary bitmap
                tempbmp.recycle();

                Log.d(TAG, "Face detection took " + (System.currentTimeMillis() - millis) + "ms");

                if (onImageLoadListener != null) {
                    onImageLoadListener.onImageLoaded(bitmap, facePos);
                }
                return null;
            }
            @Override
            public void onCatch(Throwable caught) {
                // it's not super important to do anything if face detection fails,
                // but let our listener know about it anyway:
                if (onImageLoadListener != null) {
                    onImageLoadListener.onImageFailed();
                }
            }
        }).execute();
        // and, of course, set the original bitmap as our image
        this.setImageBitmap(bitmap);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        if (onImageLoadListener != null) {
            onImageLoadListener.onImageFailed();
        }
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }


    public interface OnImageLoadListener {
        void onImageLoaded(Bitmap bitmap, PointF faceLocation);
        void onImageFailed();
    }

    private OnImageLoadListener onImageLoadListener;

    public void setOnImageLoadListener(OnImageLoadListener listener) {
        onImageLoadListener = listener;
    }
}