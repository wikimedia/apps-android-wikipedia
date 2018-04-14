package org.wikipedia.settings.languages;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class WikipediaLanguagesItemView extends FrameLayout {
    public interface Callback {
        void onCheckedChanged(int position);
    }

    @BindView(R.id.wiki_language_order) TextView orderView;
    @BindView(R.id.wiki_language_checkbox) CheckBox checkBox;
    @BindView(R.id.wiki_language_title) TextView titleView;
    @BindView(R.id.wiki_language_drag_handle) View dragHandleView;
    @Nullable private Callback callback;
    private int position;

    public WikipediaLanguagesItemView(Context context) {
        super(context);
        init();
    }

    public WikipediaLanguagesItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WikipediaLanguagesItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setContents(String languageLocalizedName, int position) {
        this.position = position;
        orderView.setText(String.valueOf(position + 1));
        titleView.setText(languageLocalizedName);
    }

    public void setCheckBoxEnabled(boolean enabled) {
        orderView.setVisibility(enabled ? GONE : VISIBLE);
        checkBox.setVisibility(enabled ? VISIBLE : GONE);
        dragHandleView.setVisibility(enabled ? GONE : VISIBLE);
        if (!enabled) {
            checkBox.setChecked(false);
        }
    }

    public void setDragHandleTouchListener(OnTouchListener listener) {
        dragHandleView.setOnTouchListener(listener);
    }

    private void init() {
        inflate(getContext(), R.layout.item_wikipedia_language, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @OnCheckedChanged(R.id.wiki_language_checkbox) void onCheckedChanged() {
        if (callback != null) {
            callback.onCheckedChanged(position);
        }
    }
}
