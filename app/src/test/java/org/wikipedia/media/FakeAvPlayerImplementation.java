package org.wikipedia.media;

import android.support.annotation.NonNull;

class FakeAvPlayerImplementation implements AvPlayerImplementation {
    private boolean asyncLoadFailure;
    private boolean syncLoadFailure;
    private boolean asyncPlayFailure;
    private boolean syncPlayFailure;

    public void setAsyncLoadFailure(boolean enabled) {
        asyncLoadFailure = enabled;
    }

    public void setSyncLoadFailure(boolean enabled) {
        syncLoadFailure = enabled;
    }

    public void setAsyncPlayFailure(boolean enabled) {
        asyncPlayFailure = enabled;
    }

    public void setSyncPlayFailure(boolean enabled) {
        syncPlayFailure = enabled;
    }

    @Override public void deinit() { }

    @Override public void init() { }

    @Override
    public void load(@NonNull String path,
                     @NonNull AvPlayer.Callback callback,
                     @NonNull AvPlayer.ErrorCallback errorCallback) {
        if (asyncLoadFailure) {
            // no callback
        } else if (syncLoadFailure) {
            errorCallback.onError();
        } else {
            callback.onSuccess();
        }
    }

    @Override public void stop() { }

    @Override
    public void play(@NonNull AvPlayer.Callback callback,
                     @NonNull AvPlayer.ErrorCallback errorCallback) {
        if (asyncPlayFailure) {
            // no callback
        } else if (syncPlayFailure) {
            errorCallback.onError();
        } else {
            callback.onSuccess();
        }
    }

    @Override public void pause() { }
}
