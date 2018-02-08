package org.wikipedia.media;

import android.support.annotation.NonNull;

public class DefaultAvPlayer implements AvPlayer {
    @NonNull
    private final AvPlayerImplementation player;
    @NonNull
    private final State state = new State();

    public DefaultAvPlayer(@NonNull AvPlayerImplementation player) {
        this.player = player;
    }

    @Override
    public void deinit() {
        if (state.isInit()) {
            player.deinit();
            state.setDeinit();
        }
    }

    @Override
    public void init() {
        if (state.isDeinit()) {
            player.init();
            state.setInit();
        }
    }

    @Override
    public void load(@NonNull String path,
                     @NonNull final Callback callback,
                     @NonNull final ErrorCallback errorCallback) {
        init();
        if (!state.isLoading(path) && !state.isLoaded(path)) {
            state.setLoading(path);
            player.load(path, () -> {
                state.setLoaded();
                if (state.isPlaying()) {
                    player.play(new StopCallbackWrapper(callback), new StopErrorCallbackWrapper(errorCallback));
                } else {
                    callback.onSuccess();
                }
            }, () -> {
                state.setInit();
                errorCallback.onError();
            });
        }
    }

    @Override
    public void stop() {
        if (state.isLoaded() && !state.isStopped()) {
            player.stop();
        }
        state.setStopped();
    }

    @Override
    public void play(@NonNull Callback callback, @NonNull ErrorCallback errorCallback) {
        if (state.isLoaded() && !state.isPlaying()) {
            state.setPlaying();
            player.play(new StopCallbackWrapper(callback), new StopErrorCallbackWrapper(errorCallback));
        } else {
            state.setPlaying();
        }
    }

    @Override
    public void play(@NonNull String path,
                     @NonNull Callback callback,
                     @NonNull ErrorCallback errorCallback) {
        play(callback, errorCallback);
        load(path, callback, errorCallback);
    }

    @Override
    public void pause() {
        if (state.isLoaded() && state.isPlaying()) {
            player.pause();
        }
        state.setPaused();
    }

    private class StopCallbackWrapper implements Callback {
        @NonNull private final Callback callback;

        StopCallbackWrapper(@NonNull Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess() {
            state.setStopped();
            callback.onSuccess();
        }
    }

    private class StopErrorCallbackWrapper implements ErrorCallback {
        @NonNull private final ErrorCallback errorCallback;

        StopErrorCallbackWrapper(@NonNull ErrorCallback errorCallback) {
            this.errorCallback = errorCallback;
        }

        @Override
        public void onError() {
            state.setStopped();
            errorCallback.onError();
        }
    }
}
