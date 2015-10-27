package org.wikipedia.crash;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.drawable.DrawableUtil;

public class CrashReportFragment extends CallbackFragment<CrashReportFragmentCallback> {
    public static CrashReportFragment newInstance() {
        return new CrashReportFragment();
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 ViewGroup container,
                                                 Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_crash_report, container, false);

        setOnClickListener(view, R.id.crash_report_start_over, new StartOverOnClickListener());
        setOnClickListener(view, R.id.crash_report_quit, new QuitOnClickListener());

        setIconColor(view.findViewById(R.id.crash_report_icon).getBackground().mutate(),
                getContrastingThemeColor());

        return view;
    }

    private void setIconColor(@NonNull Drawable icon, @ColorInt int color) {
        DrawableUtil.setTint(icon, color);
    }

    @ColorInt
    private int getContrastingThemeColor() {
        return WikipediaApp.getInstance().getContrastingThemeColor();
    }

    private void setOnClickListener(View view, @IdRes int id, View.OnClickListener listener) {
        view.findViewById(id).setOnClickListener(listener);
    }

    private class StartOverOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //noinspection ConstantConditions
            getCallback().onStartOver();
        }
    }

    private class QuitOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //noinspection ConstantConditions
            getCallback().onQuit();
        }
    }
}