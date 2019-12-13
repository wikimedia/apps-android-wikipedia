package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;

import java.util.Locale;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public final class ViewUtil {
    private static RequestOptions roundedCornersOptions = new RequestOptions().transform(new RoundedCorners(DimenUtil.roundedDpToPx(2)));

    public static void loadImageUrlInto(@NonNull ImageView drawee, @Nullable String url, boolean roundedCorners) {
        Glide.with(drawee)
                .load(isImageDownloadEnabled() && !TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                // TODO: the rounded-corners transform is applied *before* the "centerCrop" transform specified in XML.
                // we should move the centerCrop transform out of XML and into here.
                .apply(roundedCornersOptions)
                .into(drawee);
    }

    public static void loadImageUrlInto(@NonNull ImageView drawee, @Nullable String url) {
        Glide.with(drawee)
                .load(isImageDownloadEnabled() && !TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                .into(drawee);
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
