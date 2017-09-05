package org.wikipedia.theme;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.AppearanceChangeFunnel;
import org.wikipedia.events.WebViewInvalidateEvent;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.settings.Prefs;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ThemeChooserDialog extends ExtendedBottomSheetDialogFragment {
    @BindView(R.id.buttonDefaultTextSize) TextView buttonDefaultTextSize;
    @BindView(R.id.buttonDecreaseTextSize) TextView buttonDecreaseTextSize;
    @BindView(R.id.buttonIncreaseTextSize) TextView buttonIncreaseTextSize;
    @BindView(R.id.buttonColorsLight) TextView buttonThemeLight;
    @BindView(R.id.buttonColorsDark) TextView buttonThemeDark;
    @BindView(R.id.theme_chooser_dark_mode_dim_images_switch) SwitchCompat dimImagesSwitch;
    @BindView(R.id.font_change_progress_bar) ProgressBar fontChangeProgressBar;

    public interface Callback {
        void onToggleDimImages();
    }

    private enum FontSizeAction { INCREASE, DECREASE, RESET }

    private WikipediaApp app;
    private Unbinder unbinder;
    private AppearanceChangeFunnel funnel;

    private boolean updatingFont = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_theme_chooser, container);
        unbinder = ButterKnife.bind(this, rootView);
        buttonDecreaseTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.DECREASE));
        buttonDefaultTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.RESET));
        buttonIncreaseTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.INCREASE));
        buttonThemeLight.setOnClickListener(new ThemeButtonListener(Theme.LIGHT));
        buttonThemeDark.setOnClickListener(new ThemeButtonListener(Theme.DARK));
        updateComponents();
        disableBackgroundDim();
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        app.getBus().register(this);
        funnel = new AppearanceChangeFunnel(app, app.getWikiSite());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.getBus().unregister(this);
    }

    @Subscribe public void on(WebViewInvalidateEvent event) {
        updatingFont = false;
        updateComponents();
    }

    @OnClick(R.id.theme_chooser_dark_mode_dim_images_switch)
    void onToggleDimImages() {
        boolean enabled = Prefs.shouldDimDarkModeImages();
        Prefs.setDimDarkModeImages(!enabled);
        if (callback() != null) {
            // noinspection ConstantConditions
            callback().onToggleDimImages();
        }
    }

    private void updateComponents() {
        updateFontSize();
        updateThemeButtons();
        updateDimImagesSwitch();
    }

    private void updateFontSize() {
        int mult = Prefs.getTextSizeMultiplier();
        if (updatingFont) {
            fontChangeProgressBar.setVisibility(View.VISIBLE);
            buttonDefaultTextSize.setEnabled(false);
            buttonDecreaseTextSize.setEnabled(false);
            buttonIncreaseTextSize.setEnabled(false);
        } else {
            fontChangeProgressBar.setVisibility(View.GONE);
            if (mult == 0) {
                buttonDefaultTextSize.setEnabled(false);
                buttonDecreaseTextSize.setEnabled(true);
                buttonIncreaseTextSize.setEnabled(true);
            } else {
                buttonDefaultTextSize.setEnabled(true);
                buttonDecreaseTextSize.setEnabled(mult > WikipediaApp.FONT_SIZE_MULTIPLIER_MIN);
                buttonIncreaseTextSize.setEnabled(mult < WikipediaApp.FONT_SIZE_MULTIPLIER_MAX);
            }
        }
    }

    private void updateThemeButtons() {
        buttonThemeLight.setActivated(app.isCurrentThemeLight());
        buttonThemeDark.setActivated(app.isCurrentThemeDark());
    }

    private void updateDimImagesSwitch() {
        dimImagesSwitch.setChecked(Prefs.shouldDimDarkModeImages());
        dimImagesSwitch.setEnabled(app.getCurrentTheme() == Theme.DARK);
    }

    private final class ThemeButtonListener implements View.OnClickListener {
        private Theme theme;

        private ThemeButtonListener(Theme theme) {
            this.theme = theme;
        }

        @Override
        public void onClick(View v) {
            if (app.getCurrentTheme() != theme) {
                app.setCurrentTheme(theme);
                funnel.logThemeChange(app.getCurrentTheme(), theme);
            }
        }
    }

    private final class FontSizeButtonListener implements View.OnClickListener {
        private FontSizeAction action;

        private FontSizeButtonListener(FontSizeAction action) {
            this.action = action;
        }

        @Override
        public void onClick(View view) {
            updatingFont = true;
            float currentSize = app.getFontSize(getDialog().getWindow());
            if (action == FontSizeAction.INCREASE) {
                app.setFontSizeMultiplier(Prefs.getTextSizeMultiplier() + 1);
            } else if (action == FontSizeAction.DECREASE) {
                app.setFontSizeMultiplier(Prefs.getTextSizeMultiplier() - 1);
            } else if (action == FontSizeAction.RESET) {
                app.setFontSizeMultiplier(0);
            }
            updateFontSize();
            funnel.logFontSizeChange(currentSize, app.getFontSize(getDialog().getWindow()));
        }
    }

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
