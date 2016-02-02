package org.wikipedia.views;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ActionMode;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewManager;
import android.view.animation.Animation;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.WikipediaApp;
import org.wikipedia.util.ApiUtil;

import java.lang.reflect.Field;

public final class ViewUtil {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    public static void setBackgroundDrawable(View view, Drawable drawable) {
        if (ApiUtil.hasJellyBean()) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static boolean detach(@Nullable View view) {
        if (view != null && view.getParent() instanceof ViewManager) {
            ((ViewManager) view.getParent()).removeView(view);
            return true;
        }
        return false;
    }

    public static void setBottomPaddingDp(View view, int padding) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                (int) (padding * view.getContext().getResources().getDisplayMetrics().density));
    }

    public static void setAnimationMatrix(View view, Animation animation) {
        // View.setAnimationMatrix() is hidden so we can't get the final Animation frame
        // Transformation Matrix and apply it manually.
        view.clearAnimation();
        animation.setDuration(0);
        view.setAnimation(animation);
    }

    /**
     * Find the originating view of an ActionMode.
     * @param mode The ActionMode in question.
     * @return The view from which the ActionMode originated.
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static View getOriginatingView(ActionMode mode) throws NoSuchFieldException, IllegalAccessException {
        Field originatingView = mode.getClass().getDeclaredField("mOriginatingView");
        originatingView.setAccessible(true);
        return (View) originatingView.get(mode);
    }

    public static void loadImageUrlInto(@NonNull SimpleDraweeView drawee, @Nullable String url) {
        drawee.setController(Fresco.newDraweeControllerBuilder()
                .setUri(WikipediaApp.getInstance().isImageDownloadEnabled()
                        && !TextUtils.isEmpty(url)
                        ? Uri.parse(url) : null)
                .setAutoPlayAnimations(true)
                .build());
    }

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;
    }

    private ViewUtil() { }
}
