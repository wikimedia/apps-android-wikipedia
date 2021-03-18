package org.wikipedia.page;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.wikipedia.WikipediaApp;

/**
 * Descendant of BottomSheetDialogFragment that adds a few features and conveniences.
 */
public class ExtendedBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected void disableBackgroundDim() {
        getDialog().getWindow().setDimAmount(0f);
    }

    protected void setNavigationBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean isDarkThemeOrDarkBackground = WikipediaApp.getInstance().getCurrentTheme().isDark()
                    || color == ContextCompat.getColor(requireContext(), android.R.color.black);
            requireDialog().getWindow().setNavigationBarColor(color);
            requireDialog().getWindow().getDecorView().setSystemUiVisibility(isDarkThemeOrDarkBackground
                    ? getDialog().getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | getDialog().getWindow().getDecorView().getSystemUiVisibility());
        }
    }
}
