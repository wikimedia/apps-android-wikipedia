package org.wikipedia.media;

public interface AvPlayer {
    void init();
    void deinit();
    void load(String path);
    void play();
    void togglePlayback();
    void stop();
    void seek(int millis);
}