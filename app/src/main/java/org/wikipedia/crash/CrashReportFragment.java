package org.wikipedia.crash;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;

public class CrashReportFragment extends Fragment {
    public interface Callback {
        void onStartOver();
        void onQuit();
    }

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
        return view;
    }

    private void setOnClickListener(View view, @IdRes int id, View.OnClickListener listener) {
        view.findViewById(id).setOnClickListener(listener);
    }

    @Nullable private Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
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
