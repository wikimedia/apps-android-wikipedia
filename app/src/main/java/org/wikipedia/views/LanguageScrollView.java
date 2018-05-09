package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.ResourceUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER;
import static org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER;
import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class LanguageScrollView extends ConstraintLayout {
    public interface Callback {
        void onLanguageTabSelected(String selectedLanguageCode);
        void onLanguageButtonClicked();
    }

    @BindView(R.id.horizontal_scroll_languages) TabLayout horizontalLanguageScroll;
    @BindView(R.id.more_languages) TextView moreButton;
    @BindView(R.id.horizontal_scroll_languages_gradient) View gradientView;
    private Callback callback;
    private List<String> languagecodes;

    public LanguageScrollView(Context context) {
        super(context);
        init(context);
    }

    public LanguageScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LanguageScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_language_scroll, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        horizontalLanguageScroll.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateTabView(true, tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                updateTabView(false, tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateTabView(true, tab);
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (gradientView.getBackground() == null) {
            gradientView.setBackground(new GradientDrawable(ViewCompat.getLayoutDirection(this) == LAYOUT_DIRECTION_LTR
                    ? GradientDrawable.Orientation.LEFT_RIGHT : GradientDrawable.Orientation.RIGHT_LEFT,
                    new int[]{Color.TRANSPARENT, ResourceUtil.getThemedColor(getContext(), R.attr.page_toolbar_color)}));
        }
    }

    private void updateTabView(boolean selected, TabLayout.Tab tab) {
        if (selected) {
            View view = tab.getCustomView();
            if (view != null) {
                @ColorInt int color = getThemedColor(getContext(), R.attr.colorAccent);
                @ColorInt int paperColor = getThemedColor(getContext(), R.attr.paper_color);
                Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.lang_button_shape);
                updateTabLanguageCode(view, null, paperColor, drawable, color);
                updateTabLanguageLabel(view, null, color);
            }
            if (callback != null) {
                callback.onLanguageTabSelected(languagecodes.get(tab.getPosition()));
            }
        } else {
            View view = tab.getCustomView();
            if (view != null) {
                @ColorInt int color = getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color);
                updateTabLanguageLabel(view, null, color);
                updateTabLanguageCode(view, null, color, ContextCompat.getDrawable(getContext(), R.drawable.lang_button_shape_border), color);
            }
        }
    }

    public void setUpLanguageScrollTabData(List<String> languageCodes, Callback callback, int position) {
        if (this.callback != null) {
            this.callback = null;
        }
        if (horizontalLanguageScroll.getChildCount() > 0) {
            horizontalLanguageScroll.removeAllTabs();
        }
        this.callback = callback;
        this.languagecodes = languageCodes;
        for (int i = 0; i < languageCodes.size(); i++) {
            TabLayout.Tab tab = horizontalLanguageScroll.newTab();
            tab.setCustomView(createLanguageTab(languageCodes.get(i)));
            horizontalLanguageScroll.addTab(tab);
            updateTabView(false, tab);
        }
        selectLanguageTab(position);
    }

    @NonNull
    private View createLanguageTab(@NonNull String languageCode) {
        LinearLayout tab = (LinearLayout) LayoutInflater.from(this.getContext()).inflate(R.layout.view_custom_language_tab, null);
        updateTabLanguageCode(tab, languageCode, null, null, null);
        updateTabLanguageLabel(tab, languageCode, null);
        return tab;
    }

    private void updateTabLanguageLabel(@NonNull View customView, @Nullable String languageCode, @ColorInt Integer textcolor) {
        TextView languageLabelTextView = customView.findViewById(R.id.language_label);
        if (languageCode != null) {
            languageLabelTextView.setText(WikipediaApp.getInstance().language().getAppLanguageLocalizedName(languageCode));
        }
        if (textcolor != null) {
            languageLabelTextView.setTextColor(textcolor);
        }
    }

    private void updateTabLanguageCode(@NonNull View customView, @Nullable String languageCode, @ColorInt Integer textColor, @Nullable Drawable background, @ColorInt Integer backgroundColorTint) {
        TextView languageCodeTextView = customView.findViewById(R.id.language_code);
        if (languageCode != null) {
            languageCodeTextView.setText(languageCode);
            ViewUtil.formatLangButton(languageCodeTextView, languageCode, LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER);
        }
        if (textColor != null) {
            languageCodeTextView.setTextColor(textColor);
        }
        if (background != null) {
            languageCodeTextView.setBackground(background);
        }
        if (backgroundColorTint != null) {
            languageCodeTextView.getBackground().setColorFilter(backgroundColorTint, PorterDuff.Mode.SRC_IN);
        }
    }

    @OnClick(R.id.more_languages)
    void onLangButtonClick() {
        if (callback != null) {
            callback.onLanguageButtonClicked();
        }
    }

    public void selectLanguageTab(int position) {
        if (horizontalLanguageScroll != null && horizontalLanguageScroll.getTabAt(position) != null) {
            horizontalLanguageScroll.getTabAt(position).select();
        }
    }
}
