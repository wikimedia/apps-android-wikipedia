package org.wikipedia.media;

import android.media.MediaPlayer;
import android.support.annotation.NonNull;

import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;

public class MediaPlayerImplementation implements AvPlayerImplementation {
    private static final boolean VERBOSE = false;

    @NonNull private final MediaPlayer player = new MediaPlayer();

    @Override
    public void deinit() {
        if (VERBOSE) {
            L.v("Releasing");
        }
        player.release();
    }

    @Override
    public void init() {
    }

    @Override
    public void load(@NonNull String path,
                     @NonNull AvPlayer.Callback callback,
                     @NonNull AvPlayer.ErrorCallback errorCallback) {
        load(path, new PreparedListenerCallbackWrapper(callback),
                new ErrorListenerErrorCallbackWrapper(errorCallback));
    }

    @Override
    public void stop() {
        if (VERBOSE) {
            L.v("Stopping");
        }

        // Do not call MediaPlayer.stop(). This requires going through the whole lifecycle again.
        // Also, seek triggers playback, so call before pausing.
        player.seekTo(0);
        pause();
    }

    @Override
    public void play(@NonNull AvPlayer.Callback callback,
                     @NonNull AvPlayer.ErrorCallback errorCallback) {
        play(new CompletionListenerCallbackWrapper(callback),
                new ErrorListenerErrorCallbackWrapper(errorCallback));
    }

    @Override
    public void pause() {
        if (VERBOSE) {
            L.v("Pausing");
        }
        player.pause();
    }

    private void load(@NonNull String path,
                      @NonNull MediaPlayer.OnPreparedListener listener,
                      @NonNull MediaPlayer.OnErrorListener errorListener) {
        if (VERBOSE) {
            L.v("Loading path=" + path);
        }
        player.reset();
        player.setOnPreparedListener(listener);
        player.setOnErrorListener(errorListener);
        if (setDataSource(path)) {
            player.prepareAsync();
        } else {
            errorListener.onError(player, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private void play(@NonNull MediaPlayer.OnCompletionListener listener,
                      @NonNull MediaPlayer.OnErrorListener errorListener) {
        if (VERBOSE) {
            L.v("Playing");
        }
        player.setOnCompletionListener(listener);
        player.setOnErrorListener(errorListener);
        player.start();
    }

    private boolean setDataSource(@NonNull String path) {
        try {
            player.setDataSource(path);
            return true;
        } catch (IOException e) {
            L.d(e);
            return false;
        }
    }

    private abstract static class CallbackWrapper {
        @NonNull private final AvPlayer.Callback callback;
        CallbackWrapper(@NonNull AvPlayer.Callback callback) {
            this.callback =  callback;
        }

        protected void onSuccess() {
            callback.onSuccess();
        }
    }

    private abstract static class ErrorCallbackWrapper {
        @NonNull private final AvPlayer.ErrorCallback errorCallback;
        ErrorCallbackWrapper(@NonNull AvPlayer.ErrorCallback errorCallback) {
            this.errorCallback =  errorCallback;
        }

        protected void onError() {
            errorCallback.onError();
        }
    }

    private static class PreparedListenerCallbackWrapper extends CallbackWrapper implements
            MediaPlayer.OnPreparedListener {
        PreparedListenerCallbackWrapper(@NonNull AvPlayer.Callback callback) {
            super(callback);
        }

        @Override
        public void onPrepared(MediaPlayer player) {
            if (VERBOSE) {
                L.v("Loaded");
            }
            onSuccess();
        }
    }

    private static class CompletionListenerCallbackWrapper extends CallbackWrapper implements
            MediaPlayer.OnCompletionListener {
        CompletionListenerCallbackWrapper(@NonNull AvPlayer.Callback callback) {
            super(callback);
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (VERBOSE) {
                L.v("Stopped");
            }
            onSuccess();
        }
    }

    private static class ErrorListenerErrorCallbackWrapper extends ErrorCallbackWrapper implements
            MediaPlayer.OnErrorListener {
        ErrorListenerErrorCallbackWrapper(@NonNull AvPlayer.ErrorCallback errorCallback) {
            super(errorCallback);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if (VERBOSE) {
                L.v("Error: what=" + StringUtil.intToHexStr(what) + " extra=" + StringUtil.intToHexStr(extra));
            }
            onError();
            return true;
        }
    }
}
