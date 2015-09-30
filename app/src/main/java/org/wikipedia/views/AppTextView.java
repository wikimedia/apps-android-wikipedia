package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

public class AppTextView extends ConfigurableTextView {
    public AppTextView(Context context) {
        this(context, null);
    }

    public AppTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AppTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AppTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        remeasureForLineSpacing();
    }

    // Ensure the descenders of the final line are not truncated. This usually happens when
    // lineSpacingMultiplier is less than one.
    private void remeasureForLineSpacing() {
        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + calculateExtraDescenderSpace());
    }

    private int calculateExtraDescenderSpace() {
        return Math.max(0, getIntrinsicLineHeight() - getLineHeight());
    }

    /** @return Line height without space multiplication and extra spacing addition. */
    private int getIntrinsicLineHeight() {
        return getPaint().getFontMetricsInt(null);
    }
}