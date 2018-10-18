package org.wikipedia.views;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.TypedValue;

public class TabCountsView extends AppCompatTextView {

    private static final float TAB_COUNT_LARGE_NUMBER = 99;
    private static final float TAB_COUNT_SMALL_NUMBER = 9;
    private static final float TAB_COUNT_TEXT_SIZE_LARGE = 12;
    private static final float TAB_COUNT_TEXT_SIZE_MEDIUM = 10;
    private static final float TAB_COUNT_TEXT_SIZE_SMALL = 8;

    public TabCountsView(Context context) {
        super(context);
    }

    public TabCountsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TabCountsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTabCount(int count) {
        setText(String.valueOf(count));

        float tabTextSize = TAB_COUNT_TEXT_SIZE_MEDIUM;

        if (count > TAB_COUNT_LARGE_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_SMALL;
        } else if (count <= TAB_COUNT_SMALL_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_LARGE;
        }

        setTextSize(TypedValue.COMPLEX_UNIT_SP, tabTextSize);
    }
}
