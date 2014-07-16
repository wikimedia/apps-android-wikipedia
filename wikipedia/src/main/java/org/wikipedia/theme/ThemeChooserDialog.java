package org.wikipedia.theme;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.*;
import android.widget.Button;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public class ThemeChooserDialog extends Dialog {
    private WikipediaApp app;
    private Button buttonDefaultTextSize;
    private Button buttonDecreaseTextSize;
    private Button buttonIncreaseTextSize;
    private Button buttonThemeLight;
    private Button buttonThemeDark;

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
                app.setFontSizeMultiplier(app.getFontSizeMultiplier() - 1);
                updateButtonState();
            }
        });

        buttonDefaultTextSize = (Button) dlgLayout.findViewById(R.id.buttonDefaultTextSize);
        buttonDefaultTextSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app.setFontSizeMultiplier(0);
                updateButtonState();
            }
        });

        buttonIncreaseTextSize = (Button) dlgLayout.findViewById(R.id.buttonIncreaseTextSize);
        buttonIncreaseTextSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app.setFontSizeMultiplier(app.getFontSizeMultiplier() + 1);
                updateButtonState();
            }
        });

        buttonThemeLight = (Button) dlgLayout.findViewById(R.id.buttonColorsLight);
        buttonThemeLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app.setCurrentTheme(WikipediaApp.THEME_LIGHT);
            }
        });

        buttonThemeDark = (Button) dlgLayout.findViewById(R.id.buttonColorsDark);
        buttonThemeDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app.setCurrentTheme(WikipediaApp.THEME_DARK);
            }
        });

        updateButtonState();
    }

    private void updateButtonState() {
        int mult = app.getFontSizeMultiplier();
        if (mult == 0) {
            buttonDefaultTextSize.setEnabled(false);
            buttonDecreaseTextSize.setEnabled(true);
            buttonIncreaseTextSize.setEnabled(true);
        } else {
            buttonDefaultTextSize.setEnabled(true);
            buttonDecreaseTextSize.setEnabled(mult > WikipediaApp.FONT_SIZE_MULTIPLIER_MIN);
            buttonIncreaseTextSize.setEnabled(mult < WikipediaApp.FONT_SIZE_MULTIPLIER_MAX);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            buttonThemeLight.setActivated(app.getCurrentTheme() == WikipediaApp.THEME_LIGHT);
            buttonThemeDark.setActivated(app.getCurrentTheme() == WikipediaApp.THEME_DARK);
        }
    }
}
