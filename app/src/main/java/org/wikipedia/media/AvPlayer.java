package org.wikipedia.media;

import android.support.annotation.NonNull;

public interface AvPlayer {
    interface Callback {
        void onSuccess();
    }

    interface ErrorCallback {
        void onError();
    }

    void deinit();
    void init();

    void load(@NonNull String path,
              @NonNull Callback callback,
              @NonNull ErrorCallback errorCallback);

    void stop();

    void play(@NonNull Callback callback, @NonNull ErrorCallback errorCallback);
    void play(@NonNull String path,
              @NonNull Callback callback,
              @NonNull ErrorCallback errorCallback);

    void pause();
}
