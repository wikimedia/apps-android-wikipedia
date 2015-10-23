package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import org.wikipedia.util.DimenUtil;

/**
 * Blank View that properly sizes itself to the same height as the translucent status bar for
 * offsetting.
 */
public class StatusBarBlankView extends View {
    public StatusBarBlankView(Context context) {
        this(context, null);
    }

    public StatusBarBlankView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarBlankView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StatusBarBlankView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * The super implementation uses {@link View#getDefaultSize} which defaults to maximum allowed
     * size for wrap_content ({@link MeasureSpec#AT_MOST}). This implementation mimics
     * {@link android.widget.FrameLayout}'s behavior except without children.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSizeAndState(getSuggestedMinimumWidth(), widthMeasureSpec, 0),
                resolveSizeAndState(getSuggestedMinimumHeight(), heightMeasureSpec, 0));
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(), getTranslucentStatusBarHeightPx());
    }

    private int getTranslucentStatusBarHeightPx() {
        return DimenUtil.getTranslucentStatusBarHeightPx(getContext());
    }
}