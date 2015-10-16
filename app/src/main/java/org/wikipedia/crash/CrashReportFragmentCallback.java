package org.wikipedia.crash;

import org.wikipedia.activity.FragmentCallback;

public interface CrashReportFragmentCallback extends FragmentCallback {
    void onStartOver();
    void onQuit();
}