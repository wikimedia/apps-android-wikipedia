package org.wikipedia.theme;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.AppearanceChangeFunnel;
import org.wikipedia.events.WebViewInvalidateEvent;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DiscreteSeekBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.Unbinder;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class ThemeChooserDialog extends ExtendedBottomSheetDialogFragment {
    @BindView(R.id.buttonDecreaseTextSize) TextView buttonDecreaseTextSize;
    @BindView(R.id.buttonIncreaseTextSize) TextView buttonIncreaseTextSize;
    @BindView(R.id.text_size_percent) TextView textSizePercent;
    @BindView(R.id.text_size_seek_bar) DiscreteSeekBar textSizeSeekBar;
    @BindView(R.id.button_theme_light) TextView buttonThemeLight;
    @BindView(R.id.button_theme_dark) TextView buttonThemeDark;
    @BindView(R.id.button_theme_black) TextView buttonThemeBlack;
    @BindView(R.id.button_theme_sepia) TextView buttonThemeSepia;
    @BindView(R.id.button_font_family_sans_serif) MaterialButton buttonFontFamilySansSerif;
    @BindView(R.id.button_font_family_serif) MaterialButton buttonFontFamilySerif;
    @BindView(R.id.button_theme_light_highlight) View buttonThemeLightHighlight;
    @BindView(R.id.button_theme_dark_highlight) View buttonThemeDarkHighlight;
    @BindView(R.id.button_theme_black_highlight) View buttonThemeBlackHighlight;
    @BindView(R.id.button_theme_sepia_highlight) View buttonThemeSepiaHighlight;
    @BindView(R.id.theme_chooser_dark_mode_dim_images_switch) SwitchCompat dimImagesSwitch;
    @BindView(R.id.theme_chooser_match_system_theme_switch) SwitchCompat matchSystemThemeSwitch;
    @BindView(R.id.font_change_progress_bar) ProgressBar fontChangeProgressBar;

    public interface Callback {
        void onToggleDimImages();
        void onCancel();
    }

    private enum FontSizeAction { INCREASE, DECREASE, RESET }

    private WikipediaApp app;
    private Unbinder unbinder;
    private AppearanceChangeFunnel funnel;
    private Constants.InvokeSource invokeSource;
    private CompositeDisposable disposables = new CompositeDisposable();

    private boolean updatingFont = false;

    public static ThemeChooserDialog newInstance(@NonNull Constants.InvokeSource source) {
        ThemeChooserDialog dialog = new ThemeChooserDialog();
        Bundle args = new Bundle();
        args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, source);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_theme_chooser, container);
        unbinder = ButterKnife.bind(this, rootView);
        buttonDecreaseTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.DECREASE));
        buttonIncreaseTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.INCREASE));
        FeedbackUtil.setButtonLongPressToast(buttonDecreaseTextSize, buttonIncreaseTextSize);
        buttonThemeLight.setOnClickListener(new ThemeButtonListener(Theme.LIGHT));
        buttonThemeDark.setOnClickListener(new ThemeButtonListener(Theme.DARK));
        buttonThemeBlack.setOnClickListener(new ThemeButtonListener(Theme.BLACK));
        buttonThemeSepia.setOnClickListener(new ThemeButtonListener(Theme.SEPIA));
        buttonFontFamilySansSerif.setOnClickListener(new FontFamilyListener());
        buttonFontFamilySerif.setOnClickListener(new FontFamilyListener());

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int currentMultiplier = Prefs.getTextSizeMultiplier();
                boolean changed = app.setFontSizeMultiplier(textSizeSeekBar.getValue());
                if (changed) {
                    updatingFont = true;
                    updateFontSize();
                    funnel.logFontSizeChange(currentMultiplier, Prefs.getTextSizeMultiplier());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        updateComponents();
        if (!(requireActivity() instanceof PageActivity)) {
            disableBackgroundDim();
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior.from((View) getView().getParent()).setPeekHeight(DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.themeChooserSheetPeekHeight)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        invokeSource = (Constants.InvokeSource) getArguments().getSerializable(INTENT_EXTRA_INVOKE_SOURCE);
        disposables.add(app.getBus().subscribe(new EventBusConsumer()));
        funnel = new AppearanceChangeFunnel(app, app.getWikiSite(), invokeSource);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (callback() != null) {
            // noinspection ConstantConditions
            callback().onCancel();
        }
    }

    @OnCheckedChanged(R.id.theme_chooser_dark_mode_dim_images_switch)
    void onToggleDimImages(boolean enabled) {
        if (enabled == Prefs.shouldDimDarkModeImages()) {
            return;
        }
        Prefs.setDimDarkModeImages(enabled);
        if (callback() != null) {
            // noinspection ConstantConditions
            callback().onToggleDimImages();
        }
    }

    @OnCheckedChanged(R.id.theme_chooser_match_system_theme_switch)
    void onToggleMatchSystemTheme(boolean enabled) {
        if (enabled == Prefs.shouldMatchSystemTheme()) {
            return;
        }
        Prefs.setMatchSystemTheme(enabled);
        Theme currentTheme = app.getCurrentTheme();
        if (isMatchingSystemThemeEnabled()) {
            switch (WikipediaApp.getInstance().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    if (!WikipediaApp.getInstance().getCurrentTheme().isDark()) {
                        app.setCurrentTheme(!app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark() ? Theme.BLACK : app.unmarshalTheme(Prefs.getPreviousThemeId()));
                        Prefs.setPreviousThemeId(currentTheme.getMarshallingId());
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    if (WikipediaApp.getInstance().getCurrentTheme().isDark()) {
                        app.setCurrentTheme(app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark() ? Theme.LIGHT : app.unmarshalTheme(Prefs.getPreviousThemeId()));
                        Prefs.setPreviousThemeId(currentTheme.getMarshallingId());
                    }
                    break;
                default:
                    break;
            }
        }
        conditionallyDisableThemeButtons();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void conditionallyDisableThemeButtons() {
        buttonThemeLight.setAlpha((isMatchingSystemThemeEnabled() && (WikipediaApp.getInstance().getCurrentTheme().isDark())) ? 0.2f : 1.0f);
        buttonThemeSepia.setAlpha((isMatchingSystemThemeEnabled() && (WikipediaApp.getInstance().getCurrentTheme().isDark())) ? 0.2f : 1.0f);
        buttonThemeDark.setAlpha((isMatchingSystemThemeEnabled() && (!WikipediaApp.getInstance().getCurrentTheme().isDark())) ? 0.2f : 1.0f);
        buttonThemeBlack.setAlpha((isMatchingSystemThemeEnabled() && (!WikipediaApp.getInstance().getCurrentTheme().isDark())) ? 0.2f : 1.0f);

        buttonThemeLight.setEnabled(!isMatchingSystemThemeEnabled() || (!WikipediaApp.getInstance().getCurrentTheme().isDark()));
        buttonThemeSepia.setEnabled(!isMatchingSystemThemeEnabled() || (!WikipediaApp.getInstance().getCurrentTheme().isDark()));
        buttonThemeDark.setEnabled(!isMatchingSystemThemeEnabled() || (WikipediaApp.getInstance().getCurrentTheme().isDark()));
        buttonThemeBlack.setEnabled(!isMatchingSystemThemeEnabled() || (WikipediaApp.getInstance().getCurrentTheme().isDark()));
    }

    private boolean isMatchingSystemThemeEnabled() {
        return Prefs.shouldMatchSystemTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private void updateComponents() {
        updateFontSize();
        updateFontFamily();
        updateThemeButtons();
        updateDimImagesSwitch();
        updateMatchSystemThemeSwitch();
    }

    private void updateMatchSystemThemeSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            matchSystemThemeSwitch.setVisibility(View.VISIBLE);
            matchSystemThemeSwitch.setChecked(Prefs.shouldMatchSystemTheme());
            conditionallyDisableThemeButtons();
        } else {
            matchSystemThemeSwitch.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void updateFontSize() {
        int mult = Prefs.getTextSizeMultiplier();
        textSizeSeekBar.setValue(mult);
        String percentStr = getString(R.string.text_size_percent,
                (int) (100 * (1 + mult * DimenUtil.getFloat(R.dimen.textSizeMultiplierFactor))));
        textSizePercent.setText(mult == 0
                ? getString(R.string.text_size_percent_default, percentStr) : percentStr);
        if (updatingFont) {
            fontChangeProgressBar.setVisibility(View.VISIBLE);
        } else {
            fontChangeProgressBar.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void updateFontFamily() {
        buttonFontFamilySansSerif.setStrokeWidth(Prefs.getFontFamily().equals(buttonFontFamilySansSerif.getTag()) ? 5 : 0);
        buttonFontFamilySerif.setStrokeWidth(Prefs.getFontFamily().equals(buttonFontFamilySerif.getTag()) ? 5 : 0);
    }

    private void updateThemeButtons() {
        buttonThemeLightHighlight.setVisibility(app.getCurrentTheme() == Theme.LIGHT ? View.VISIBLE : View.GONE);
        buttonThemeLight.setClickable(app.getCurrentTheme() != Theme.LIGHT);
        buttonThemeSepiaHighlight.setVisibility(app.getCurrentTheme() == Theme.SEPIA ? View.VISIBLE : View.GONE);
        buttonThemeSepia.setClickable(app.getCurrentTheme() != Theme.SEPIA);
        buttonThemeDarkHighlight.setVisibility(app.getCurrentTheme() == Theme.DARK ? View.VISIBLE : View.GONE);
        buttonThemeDark.setClickable(app.getCurrentTheme() != Theme.DARK);
        buttonThemeBlackHighlight.setVisibility(app.getCurrentTheme() == Theme.BLACK ? View.VISIBLE : View.GONE);
        buttonThemeBlack.setClickable(app.getCurrentTheme() != Theme.BLACK);
    }

    private void updateDimImagesSwitch() {
        dimImagesSwitch.setChecked(Prefs.shouldDimDarkModeImages());
        dimImagesSwitch.setEnabled(app.getCurrentTheme().isDark());
        dimImagesSwitch.setTextColor(dimImagesSwitch.isEnabled()
                ? ResourceUtil.getThemedColor(requireContext(), R.attr.section_title_color)
                : ContextCompat.getColor(requireContext(), R.color.black26));
    }

    private final class ThemeButtonListener implements View.OnClickListener {
        private Theme theme;

        private ThemeButtonListener(Theme theme) {
            this.theme = theme;
        }

        @Override
        public void onClick(View v) {
            if (app.getCurrentTheme() != theme) {
                funnel.logThemeChange(app.getCurrentTheme(), theme);
                app.setCurrentTheme(theme);
            }
        }
    }

    private final class FontFamilyListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                String newFontFamily = (String) v.getTag();
                funnel.logFontThemeChange(Prefs.getFontFamily(), newFontFamily);
                app.setFontFamily(newFontFamily);
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
            boolean changed = false;
            int currentMultiplier = Prefs.getTextSizeMultiplier();
            if (action == FontSizeAction.INCREASE) {
                changed = app.setFontSizeMultiplier(Prefs.getTextSizeMultiplier() + 1);
            } else if (action == FontSizeAction.DECREASE) {
                changed = app.setFontSizeMultiplier(Prefs.getTextSizeMultiplier() - 1);
            } else if (action == FontSizeAction.RESET) {
                changed = app.setFontSizeMultiplier(0);
            }
            if (changed) {
                updatingFont = true;
                updateFontSize();
                funnel.logFontSizeChange(currentMultiplier, Prefs.getTextSizeMultiplier());
            }
        }
    }

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof WebViewInvalidateEvent) {
                updatingFont = false;
                updateComponents();
            }
        }
    }

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
