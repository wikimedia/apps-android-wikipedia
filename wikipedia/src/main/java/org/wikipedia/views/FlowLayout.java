package org.wikipedia.views;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a "Flow" layout, where child Views are arranged horizontally, and allowed
 * to overflow onto multiple "lines".
 */
public class FlowLayout extends ViewGroup {

    Resources resources;
    private List<Integer> lineHeights = new ArrayList<Integer>();

    private static final int HORIZONTAL_SPACING = 8;
    private static final int VERTICAL_SPACING = 8;

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
        assert (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED);

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
                int childWidth = child.getMeasuredWidth() + getDp(HORIZONTAL_SPACING);
                lineHeight = Math.max(lineHeight, child.getMeasuredHeight());

                if (xpos + childWidth > width) {
                    xpos = getPaddingLeft();
                    lineHeight += getDp(VERTICAL_SPACING);
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
                    lineHeights.add(getChildAt(getChildCount() - 1).getMeasuredHeight() + getDp(VERTICAL_SPACING));
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
        if (p instanceof LayoutParams) {
            return true;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int width = r - l;
        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();
        int curLine = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childw = child.getMeasuredWidth();
                int childh = child.getMeasuredHeight();
                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    if (lineHeights.size() > curLine) {
                        ypos += lineHeights.get(curLine++);
                    }
                }
                child.layout(xpos, ypos, xpos + childw, ypos + childh);
                xpos += childw + getDp(HORIZONTAL_SPACING);
            }
        }
    }

    private int getDp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.getDisplayMetrics()
        );
    }
}
