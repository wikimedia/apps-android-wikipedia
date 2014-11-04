package org.wikipedia.theme;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import com.squareup.otto.Subscribe;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.AppearanceChangeFunnel;
import org.wikipedia.events.WebViewInvalidateEvent;

public class ThemeChooserDialog extends Dialog {
    private WikipediaApp app;
    private Button buttonDefaultTextSize;
    private Button buttonDecreaseTextSize;
    private Button buttonIncreaseTextSize;
    private Button buttonThemeLight;
    private Button buttonThemeDark;
    private ProgressBar fontChangeProgressBar;
    private boolean updatingFont = false;
    private AppearanceChangeFunnel funnel;

    public ThemeChooserDialog(Context context) {
        super(context);
        app = WikipediaApp.getInstance();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dlgLayout = inflater.inflate(R.layout.dialog_themechooser, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().setDimAmount(0.0f);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(dlgLayout);
        funnel = new AppearanceChangeFunnel(app, app.getPrimarySite());

        getWindow().setBackgroundDrawable(null);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(lp);

        buttonDecreaseTextSize = (Button) dlgLayout.findViewById(R.id.buttonDecreaseTextSize);
        buttonDecreaseTextSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatingFont = true;
                float currentSize = app.getFontSize(getWindow());
                app.setFontSizeMultiplier(app.getFontSizeMultiplier() - 1);
                updateButtonState();
                funnel.logFontSizeChange(currentSize, app.getFontSize(getWindow()));
            }
        });

        buttonDefaultTextSize = (Button) dlgLayout.findViewById(R.id.buttonDefaultTextSize);
        buttonDefaultTextSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatingFont = true;
                float currentSize = app.getFontSize(getWindow());
                app.setFontSizeMultiplier(0);
                updateButtonState();
                funnel.logFontSizeChange(currentSize, app.getFontSize(getWindow()));
            }
        });

        buttonIncreaseTextSize = (Button) dlgLayout.findViewById(R.id.buttonIncreaseTextSize);
        buttonIncreaseTextSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatingFont = true;
                float currentSize = app.getFontSize(getWindow());
                app.setFontSizeMultiplier(app.getFontSizeMultiplier() + 1);
                updateButtonState();
                funnel.logFontSizeChange(currentSize, app.getFontSize(getWindow()));
            }
        });

        buttonThemeLight = (Button) dlgLayout.findViewById(R.id.buttonColorsLight);
        buttonThemeLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Only change the theme to light mode and log change if user is not on light mode
                if (app.getCurrentTheme() != WikipediaApp.THEME_LIGHT) {
                    funnel.logThemeChange(app.getCurrentTheme(), WikipediaApp.THEME_LIGHT);
                    app.setCurrentTheme(WikipediaApp.THEME_LIGHT);
                }
            }
        });

        buttonThemeDark = (Button) dlgLayout.findViewById(R.id.buttonColorsDark);
        buttonThemeDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Only change the theme to dark mode and log change if user is not on dark mode
                if (app.getCurrentTheme() != WikipediaApp.THEME_DARK) {
                    funnel.logThemeChange(app.getCurrentTheme(), WikipediaApp.THEME_DARK);
                    app.setCurrentTheme(WikipediaApp.THEME_DARK);
                }
            }
        });

        fontChangeProgressBar = (ProgressBar) dlgLayout.findViewById(R.id.font_change_progress_bar);

        updateButtonState();
    }

    @Subscribe
    public void onWebViewInvalidated(WebViewInvalidateEvent event) {
        updatingFont = false;
        updateButtonState();
    }

    @Override
    public void show() {
        app.getBus().register(this);
        super.show();
    }

    @Override
    public void dismiss() {
        app.getBus().unregister(this);
        super.dismiss();
    }

    private void updateButtonState() {
        int mult = app.getFontSizeMultiplier();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            buttonThemeLight.setActivated(app.getCurrentTheme() == WikipediaApp.THEME_LIGHT);
            buttonThemeDark.setActivated(app.getCurrentTheme() == WikipediaApp.THEME_DARK);
        }
    }
}
