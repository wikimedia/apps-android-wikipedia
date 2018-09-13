package org.wikipedia.page;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.page.action.PageActionTab;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.ConfigurableTabLayout;

public class PageActionTabLayout extends ConfigurableTabLayout {
    public PageActionTabLayout(Context context) {
        this(context, null);
    }

    public PageActionTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageActionTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.view_article_tab_layout, this);
    }

    public void setPageActionTabsCallback(PageActionTab.Callback pageActionTabsCallback) {
        for (int i = 0; i < getChildCount(); i++) {
            View tab = getChildAt(i);
            if (tab.getTag() != null) {
                int tabPosition = Integer.valueOf((String) tab.getTag());

                tab.setOnClickListener((v) -> {
                    if (isEnabled(v)) {
                        PageActionTab.of(tabPosition).select(pageActionTabsCallback);
                    }
                });
            }

            FeedbackUtil.setToolbarButtonLongPressToast(tab);
        }
    }

}
