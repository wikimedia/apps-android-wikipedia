package org.wikipedia.settings.languages;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnLongClick;

import static org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER;
import static org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER;

public class WikipediaLanguagesItemView extends LinearLayout {
    public interface Callback {
        void onCheckedChanged(int position);
        void onLongPress(int position);
    }

    @BindView(R.id.wiki_language_order) TextView orderView;
    @BindView(R.id.wiki_language_checkbox) CheckBox checkBox;
    @BindView(R.id.wiki_language_title) TextView titleView;
    @BindView(R.id.wiki_language_code) TextView langCodeView;
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

    public void setContents(@NonNull String langCode, @Nullable String languageLocalizedName, int position) {
        this.position = position;
        orderView.setText(String.valueOf(position + 1));
        titleView.setText(StringUtils.capitalize(languageLocalizedName));
        ViewUtil.formatLangButton(langCodeView, langCode, LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER);
        langCodeView.setText(langCode);
        langCodeView.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color));
        langCodeView.getBackground().setColorFilter(ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color),
                PorterDuff.Mode.SRC_IN);
    }

    public void setCheckBoxEnabled(boolean enabled) {
        orderView.setVisibility(enabled ? GONE : VISIBLE);
        checkBox.setVisibility(enabled ? VISIBLE : GONE);
        if (!enabled) {
            checkBox.setChecked(false);
            setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        }
    }

    public void setCheckBoxChecked(boolean checked) {
        checkBox.setChecked(checked);
        updateBackgroundColor();
    }

    public void setDragHandleEnabled(boolean enabled) {
        dragHandleView.setVisibility(enabled ? VISIBLE : GONE);
    }

    public void setDragHandleTouchListener(OnTouchListener listener) {
        dragHandleView.setOnTouchListener(listener);
    }

    private void init() {
        inflate(getContext(), R.layout.item_wikipedia_language, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setForeground(ContextCompat.getDrawable(getContext(),
                    ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground)));
        }
    }

    private void updateBackgroundColor() {
        setBackgroundColor(checkBox.isChecked()
                ? ResourceUtil.getThemedColor(getContext(), R.attr.multi_select_background_color) : ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
    }

    @OnCheckedChanged(R.id.wiki_language_checkbox) void onCheckedChanged() {
        if (callback != null) {
            callback.onCheckedChanged(position);
            updateBackgroundColor();
        }
    }

    @OnLongClick boolean onLongClick() {
        if (callback != null) {
            callback.onLongPress(position);
        }
        return true;
    }
}
