package org.wikipedia.media;

import android.support.annotation.NonNull;

interface AvPlayerImplementation {
    // May be called when initialized.
    void deinit();

    // May only be called when deinitialized.
    void init();

    // May be called when initialized.
    void load(@NonNull String path,
              @NonNull AvPlayer.Callback callback,
              @NonNull AvPlayer.ErrorCallback errorCallback);

    // May be called when playing or paused.
    void stop();

    // May be called when loaded and not playing.
    void play(@NonNull AvPlayer.Callback callback, @NonNull AvPlayer.ErrorCallback errorCallback);

    // May be called when playing.
    void pause();
}
