package org.wikipedia.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TabCountsView extends FrameLayout {

    private static final float TAB_COUNT_LARGE_NUMBER = 99;
    private static final float TAB_COUNT_SMALL_NUMBER = 9;
    private static final float TAB_COUNT_TEXT_SIZE_LARGE = 12;
    private static final float TAB_COUNT_TEXT_SIZE_MEDIUM = 10;
    private static final float TAB_COUNT_TEXT_SIZE_SMALL = 8;

    @BindView(R.id.tabs_count_text) TextView tabsCountText;

    public TabCountsView(Context context) {
        super(context);
        init();
    }

    public TabCountsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TabCountsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_tabs_count, this);
        ButterKnife.bind(this);

        final int buttonWidth = 48;
        setLayoutParams(new ViewGroup.LayoutParams(DimenUtil.roundedDpToPx(buttonWidth), ViewGroup.LayoutParams.MATCH_PARENT));
        setBackgroundResource(ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackgroundBorderless));
        setContentDescription(getContext().getString(R.string.menu_page_show_tabs));
    }

    public void updateTabCount() {
        int count = WikipediaApp.getInstance().getTabCount();
        tabsCountText.setText(String.valueOf(count));

        float tabTextSize = TAB_COUNT_TEXT_SIZE_MEDIUM;

        if (count > TAB_COUNT_LARGE_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_SMALL;
        } else if (count <= TAB_COUNT_SMALL_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_LARGE;
        }

        tabsCountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, tabTextSize);
    }

    public void setColor(@ColorInt int color) {
        tabsCountText.setTextColor(color);
        tabsCountText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
