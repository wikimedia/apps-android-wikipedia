package org.wikipedia.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

// TODO: Document where it is desirable to use this class vs. a vanilla TextView
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Workaround for https://code.google.com/p/android/issues/detail?id=191430
        // which only occurs on API 23
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    && getSelectionStart() != getSelectionEnd()) {
                CharSequence text = getText();
                setText(null);
                setText(text);
            }
        }
        return super.dispatchTouchEvent(event);
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
