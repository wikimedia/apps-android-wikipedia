package org.wikipedia.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;

import java.util.Locale;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public final class ViewUtil {
    public static boolean detach(@Nullable View view) {
        if (view != null && view.getParent() instanceof ViewManager) {
            ((ViewManager) view.getParent()).removeView(view);
            return true;
        }
        return false;
    }

    public static void setTopPaddingDp(@NonNull View view, int padding) {
        view.setPadding(view.getPaddingLeft(), (int) (padding * DimenUtil.getDensityScalar()),
                view.getPaddingRight(), view.getPaddingBottom());
    }

    public static void setBottomPaddingDp(@NonNull View view, int padding) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                (int) (padding * DimenUtil.getDensityScalar()));
    }

    public static void setAnimationMatrix(View view, Animation animation) {
        // View.setAnimationMatrix() is hidden so we can't get the final Animation frame
        // Transformation Matrix and apply it manually.
        view.clearAnimation();
        animation.setDuration(0);
        view.setAnimation(animation);
    }

    public static void loadImageUrlInto(@NonNull SimpleDraweeView drawee, @Nullable String url) {
        drawee.setController(Fresco.newDraweeControllerBuilder()
                .setUri(isImageDownloadEnabled() && !TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                .setAutoPlayAnimations(true)
                .build());
    }

    public static void setCloseButtonInActionMode(@NonNull Context context, @NonNull android.view.ActionMode actionMode) {
        View view = View.inflate(context, R.layout.view_action_mode_close_button, null);
        actionMode.setCustomView(view);
        ImageView closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> actionMode.finish());
    }

    @NonNull
    public static Bitmap getBitmapFromView(@NonNull View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;
    }

    @Nullable public static ViewGroup parent(@NonNull View view) {
        return view.getParent() instanceof ViewGroup ? (ViewGroup) view.getParent() : null;
    }

    public static void remove(@NonNull View view) {
        ViewManager parent = parent(view);
        if (parent != null) {
            parent.removeView(view);
        }
    }

    /** Replace the current View with a new View by copying the ID and LayoutParams (by reference). */
    public static void replace(@NonNull View current, @NonNull View next) {
        ViewGroup parent = parent(current);
        if (parent == null || parent(next) != null) {
            String msg = "Parent of current View must be nonnull; parent of next View must be null.";
            throw new IllegalStateException(msg);
        }

        next.setId(current.getId());
        next.setLayoutParams(current.getLayoutParams());

        int index = parent.indexOfChild(current);
        remove(current);
        parent.addView(next, index);
    }

    public static void fadeIn(@NonNull View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f)
                .setDuration(view.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);
    }

    public static void fadeOut(@NonNull final View view) {
        view.animate().alpha(0f)
                .setDuration(view.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
    }

    public static void finishActionModeWhenTappingOnView(@NonNull View view, @Nullable ActionMode actionMode) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    public static void formatLangButton(@NonNull TextView langButton, @NonNull String langCode,
                                        int langButtonTextSizeSmaller, int langButtonTextSizeLarger) {
        final int langCodeStandardLength = 3;
        final int langButtonTextMaxLength = 7;

        if (langCode.length() > langCodeStandardLength) {
            langButton.setTextSize(langButtonTextSizeSmaller);
            if (langCode.length() > langButtonTextMaxLength) {
                langButton.setText(langCode.substring(0, langButtonTextMaxLength).toUpperCase(Locale.ENGLISH));
            }
            return;
        }
        langButton.setTextSize(langButtonTextSizeLarger);
    }

    private ViewUtil() {
    }
}
