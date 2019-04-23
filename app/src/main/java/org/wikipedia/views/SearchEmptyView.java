package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchEmptyView extends LinearLayout {
    @BindView(R.id.search_empty_text) TextView emptyText;

    public SearchEmptyView(Context context) {
        super(context);
        init();
    }

    public SearchEmptyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchEmptyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SearchEmptyView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setEmptyText(@StringRes int id) {
        emptyText.setText(id);
    }

    public void setEmptyText(@Nullable CharSequence text) {
        emptyText.setText(text);
    }

    private void init() {
        inflate(getContext(), R.layout.view_search_empty, this);
        ButterKnife.bind(this);
        setOrientation(VERTICAL);
    }
}
