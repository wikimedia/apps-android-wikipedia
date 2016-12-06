package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import org.wikipedia.drawable.DrawableUtil;

import java.util.ArrayList;
import java.util.List;

// Credit: https://stackoverflow.com/a/38977396
public class AppTextViewWithImages extends AppTextView {

    public AppTextViewWithImages(Context context) {
        super(context);
    }

    public AppTextViewWithImages(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppTextViewWithImages(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AppTextViewWithImages(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * A method to set a Spanned character sequence containing drawable resources.
     *
     * @param text A CharSequence formatted for use in android.text.TextUtils.expandTemplate(),
     *             e.g.: "^1 is my favorite mobile operating system."  Placeholders are expected in
     *             the format "^1", "^2", and so on.
     * @param drawableIds Numeric drawable IDs for the drawables which are to replace the
     *                    placeholders, in the order in which they should appear.
     */
    public void setTextWithDrawables(@NonNull CharSequence text, @DrawableRes int... drawableIds) {
        setText(text, getImageSpans(drawableIds));
    }

    private List<Spanned> getImageSpans(@DrawableRes int... drawableIds) {
        List<Spanned> result = new ArrayList<>();
        for (int id : drawableIds) {
            Spanned span = makeImageSpan(id, getTextSize(), getCurrentTextColor());
            result.add(span);
        }
        return result;
    }

    private void setText(@NonNull CharSequence text, @NonNull List<Spanned> spans) {
        if (!spans.isEmpty()) {
            CharSequence spanned = TextUtils.expandTemplate(text, spans.toArray(new CharSequence[spans.size()]));
            super.setText(spanned, BufferType.SPANNABLE);
        } else {
            super.setText(text);
        }
    }

    /**
     * Create an ImageSpan containing a drawable to be inserted in a TextView. This also sets the
     * image size and color.
     *
     * @param drawableId    A drawable resource Id.
     * @param size          The desired size (i.e. width and height) of the image icon in pixels.
     * @param color         The color to apply to the image.
     * @return  A single-length ImageSpan that can be swapped into a CharSequence to replace a
     *          placeholder.
     */
    @NonNull @VisibleForTesting
    Spannable makeImageSpan(@DrawableRes int drawableId, float size, @ColorInt int color) {
        Spannable result = Spannable.Factory.getInstance().newSpannable(" ");
        Drawable drawable = getFormattedDrawable(drawableId, size, color);
        result.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, 1,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return result;
    }

    @NonNull @VisibleForTesting
    Drawable getFormattedDrawable(@DrawableRes int drawableId, float size, @ColorInt int color) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), drawableId);
        DrawableUtil.setTint(drawable, color);

        float ratio = drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, Math.round(size), Math.round(size * ratio));

        return drawable;
    }

    /* Helper method for testing */
    @NonNull @VisibleForTesting
    <T> T[] getSpans(@NonNull Class<T> clazz) {
        return ((SpannableString) getText()).getSpans(0, getText().length(), clazz);
    }
}
