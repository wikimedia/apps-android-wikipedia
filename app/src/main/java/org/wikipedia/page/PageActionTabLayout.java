package org.wikipedia.page;

import android.content.Context;
import android.util.AttributeSet;

import org.wikipedia.R;
import org.wikipedia.views.ConfigurableTabLayout;

import butterknife.ButterKnife;

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
        ButterKnife.bind(this);
    }
}
