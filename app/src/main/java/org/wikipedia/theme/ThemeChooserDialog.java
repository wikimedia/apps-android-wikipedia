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

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.AppearanceChangeFunnel;
import org.wikipedia.databinding.DialogThemeChooserBinding;
import org.wikipedia.events.WebViewInvalidateEvent;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DiscreteSeekBar;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class ThemeChooserDialog extends ExtendedBottomSheetDialogFragment {
    private DialogThemeChooserBinding binding;
    private TextView textSizePercent;
    private DiscreteSeekBar textSizeSeekBar;
    private TextView buttonThemeLight;
    private TextView buttonThemeDark;
    private TextView buttonThemeBlack;
    private TextView buttonThemeSepia;
    private View buttonThemeLightHighlight;
    private View buttonThemeDarkHighlight;
    private View buttonThemeBlackHighlight;
    private View buttonThemeSepiaHighlight;
    private SwitchCompat dimImagesSwitch;
    private SwitchCompat matchSystemThemeSwitch;
    private ProgressBar fontChangeProgressBar;

    public interface Callback {
        void onToggleDimImages();
        void onCancel();
    }

    private enum FontSizeAction { INCREASE, DECREASE, RESET }

    private WikipediaApp app;
    private AppearanceChangeFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();

    private boolean updatingFont = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogThemeChooserBinding.inflate(inflater, container, true);

        final TextView buttonDecreaseTextSize = binding.buttonDecreaseTextSize;
        final TextView buttonIncreaseTextSize = binding.buttonIncreaseTextSize;
        textSizePercent = binding.textSizePercent;
        textSizeSeekBar = binding.textSizeSeekBar;
        buttonThemeLight = binding.buttonThemeLight;
        buttonThemeDark = binding.buttonThemeDark;
        buttonThemeBlack = binding.buttonThemeBlack;
        buttonThemeSepia = binding.buttonThemeSepia;
        buttonThemeLightHighlight = binding.buttonThemeLightHighlight;
        buttonThemeDarkHighlight = binding.buttonThemeDarkHighlight;
        buttonThemeBlackHighlight = binding.buttonThemeBlackHighlight;
        buttonThemeSepiaHighlight = binding.buttonThemeSepiaHighlight;
        dimImagesSwitch = binding.themeChooserDarkModeDimImagesSwitch;
        matchSystemThemeSwitch = binding.themeChooserMatchSystemThemeSwitch;
        fontChangeProgressBar = binding.fontChangeProgressBar;

        dimImagesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked == Prefs.shouldDimDarkModeImages()) {
                return;
            }
            Prefs.setDimDarkModeImages(isChecked);
            if (callback() != null) {
                // noinspection ConstantConditions
                callback().onToggleDimImages();
            }
        });
        matchSystemThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked == Prefs.shouldMatchSystemTheme()) {
                return;
            }
            Prefs.setMatchSystemTheme(isChecked);
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
        });

        buttonDecreaseTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.DECREASE));
        buttonIncreaseTextSize.setOnClickListener(new FontSizeButtonListener(FontSizeAction.INCREASE));
        FeedbackUtil.setToolbarButtonLongPressToast(buttonDecreaseTextSize, buttonIncreaseTextSize);
        buttonThemeLight.setOnClickListener(new ThemeButtonListener(Theme.LIGHT));
        buttonThemeDark.setOnClickListener(new ThemeButtonListener(Theme.DARK));
        buttonThemeBlack.setOnClickListener(new ThemeButtonListener(Theme.BLACK));
        buttonThemeSepia.setOnClickListener(new ThemeButtonListener(Theme.SEPIA));

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
        return binding.getRoot();
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
        disposables.add(app.getBus().subscribe(new EventBusConsumer()));
        funnel = new AppearanceChangeFunnel(app, app.getWikiSite());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
