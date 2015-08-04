package org.wikipedia.views;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.BuildConfig;
import org.wikipedia.util.DimenUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a "Flow" layout, where child Views are arranged horizontally, and allowed
 * to overflow onto multiple "lines".
 */
public class FlowLayout extends ViewGroup {
    private static final int HORIZONTAL_SPACING = 8;
    private static final int VERTICAL_SPACING = 8;

    private Resources resources;
    private List<Integer> lineHeights = new ArrayList<>();

    public FlowLayout(Context context) {
        super(context);
        resources = context.getResources();
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        resources = context.getResources();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (BuildConfig.DEBUG && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw new AssertionError();
        }

        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        int count = getChildCount();
        int lineHeight = 0;

        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();

        int childHeightMeasureSpec;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        } else {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        lineHeights.clear();
        boolean overflowed = false;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), childHeightMeasureSpec);
                int childWidth = child.getMeasuredWidth() + (int) DimenUtil.dpToPx(HORIZONTAL_SPACING);
                lineHeight = Math.max(lineHeight, child.getMeasuredHeight());

                if (xpos + childWidth > width) {
                    xpos = getPaddingLeft();
                    lineHeight += (int) DimenUtil.dpToPx(VERTICAL_SPACING);
                    lineHeights.add(lineHeight);
                    ypos += lineHeight;
                    lineHeight = 0;
                    overflowed = true;
                }
                xpos += childWidth;
            }
        }
        if (overflowed) {
            if (lineHeight > 0) {
                lineHeights.add(lineHeight);
            } else {
                if (getChildCount() > 0) {
                    lineHeights.add(getChildAt(getChildCount() - 1).getMeasuredHeight() + (int) DimenUtil.dpToPx(VERTICAL_SPACING));
                }
            }
        } else {
            if (lineHeight > 0) {
                lineHeights.add(lineHeight);
            }
        }

        int totalHeight = 0;
        for (int i : lineHeights) {
            totalHeight += i;
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = totalHeight;
        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            if (totalHeight < height) {
                height = totalHeight;
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(0, 0);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int width = r - l;

        boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
        int xpos = isRtl ? width - getPaddingRight() : getPaddingLeft();
        int ypos = getPaddingTop();
        int curLine = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childw = child.getMeasuredWidth();
                int childh = child.getMeasuredHeight();
                if (isRtl) {
                    if (xpos - childw < 0) {
                        xpos = width - getPaddingRight();
                        if (lineHeights.size() > curLine) {
                            ypos += lineHeights.get(curLine++);
                        }
                    }
                    child.layout(xpos - getChildAt(i).getMeasuredWidth(), ypos,
                            xpos - getChildAt(i).getMeasuredWidth() + childw, ypos + childh);
                    xpos -= childw + (int) DimenUtil.dpToPx(HORIZONTAL_SPACING);
                } else {
                    if (xpos + childw > width) {
                        xpos = getPaddingLeft();
                        if (lineHeights.size() > curLine) {
                            ypos += lineHeights.get(curLine++);
                        }
                    }
                    child.layout(xpos, ypos, xpos + childw, ypos + childh);
                    xpos += childw + (int) DimenUtil.dpToPx(HORIZONTAL_SPACING);
                }
            }
        }
    }
}
