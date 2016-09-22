package org.wikipedia.page;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import org.wikipedia.R;

import butterknife.ButterKnife;

public class ArticleTabLayout extends TabLayout {
    public ArticleTabLayout(Context context) {
        this(context, null);
    }

    public ArticleTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArticleTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.view_article_tab_layout, this);
        ButterKnife.bind(this);
    }
}