package org.wikipedia.media;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

class State {
    private enum LoadState {
        DEINIT, INIT, LOADING, LOADED
    }

    private enum PlayState {
        STOPPED, PLAYING, PAUSED
    }

    @NonNull
    private LoadState loadState = LoadState.DEINIT;
    @NonNull
    private PlayState playState = PlayState.STOPPED;
    @Nullable
    private String path;

    @Nullable
    public String getPath() {
        return path;
    }

    public boolean isDeinit() {
        return loadState == LoadState.DEINIT;
    }

    public void setDeinit() {
        loadState = LoadState.DEINIT;
        playState = PlayState.STOPPED;
    }

    public boolean isInit() {
        return !isDeinit();
    }

    public void setInit() {
        loadState = LoadState.INIT;
    }

    public boolean isLoading() {
        return loadState == LoadState.LOADING;
    }

    public boolean isLoading(@Nullable String path) {
        return isLoading() && isPathIdentical(path);
    }

    public void setLoading(@Nullable String path) {
        if (!isLoaded(path)) {
            loadState = LoadState.LOADING;
            this.path = path;
        }
    }

    public boolean isLoaded() {
        return loadState == LoadState.LOADED;
    }

    public boolean isLoaded(@Nullable String path) {
        return isLoaded() && isPathIdentical(path);
    }

    public void setLoaded() {
        loadState = LoadState.LOADED;
    }

    public boolean isStopped() {
        return playState == PlayState.STOPPED;
    }

    public void setStopped() {
        playState = PlayState.STOPPED;
    }

    public boolean isPlaying() {
        return playState == PlayState.PLAYING;
    }

    public void setPlaying() {
        playState = PlayState.PLAYING;
    }

    public boolean isPaused() {
        return playState == PlayState.PAUSED;
    }

    public void setPaused() {
        playState = PlayState.PAUSED;
    }

    private boolean isPathIdentical(@Nullable String path) {
        return StringUtils.equals(this.path, path);
    }
}
