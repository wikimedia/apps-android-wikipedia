package org.wikipedia.media;

import android.support.annotation.NonNull;

interface AvPlayerImplementation {
    void deinit();
    void init();
    void load(@NonNull String path,
              @NonNull AvPlayer.Callback callback,
              @NonNull AvPlayer.ErrorCallback errorCallback);
    void stop();
    void play(@NonNull AvPlayer.Callback callback, @NonNull AvPlayer.ErrorCallback errorCallback);
    void pause();
}