package org.wikipedia.media;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StateTest {
    private static final String PATH_A = "http://pathA";
    private static final String PATH_B = "http://pathB";

    private State subject;

    @Before
    public void setUp() {
        subject = new State();
    }

    @Test
    public void testGetPathConstructor() {
        assertThat(subject.getPath(), is((String) null));
    }

    @Test
    public void testGetPathDeinit() {
        subject.setLoading(PATH_A);
        subject.setDeinit();
        assertPath(PATH_A);
    }

    @Test
    public void testGetPathInit() {
        subject.setLoading(PATH_A);
        subject.setInit();
        assertPath(PATH_A);
    }

    @Test
    public void testGetPathLoaded() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        assertPath(PATH_A);
    }

    @Test
    public void testIsDeinitConstructor() {
        assertDeinit(true);
    }

    @Test
    public void testIsDeinitInit() {
        subject.setInit();
        assertDeinit(false);
    }

    @Test
    public void testSetDeinitInit() {
        subject.setInit();
        subject.setDeinit();
        assertDeinit(true);
    }

    @Test
    public void testIsInitConstructor() {
        assertInit(false);
    }

    @Test
    public void testIsInitDeinit() {
        subject.setDeinit();
        assertInit(false);
    }

    @Test
    public void testIsInitInit() {
        subject.setInit();
        assertInit(true);
    }

    @Test
    public void testIsInitLoading() {
        subject.setLoading(PATH_A);
        assertInit(true);
    }

    @Test
    public void testIsInitLoaded() {
        subject.setLoaded();
        assertInit(true);
    }

    @Test
    public void testSetInitDeinit() {
        subject.setDeinit();
        subject.setInit();
        assertInit(true);
    }

    @Test
    public void testIsLoadingConstructor() {
        assertThat(subject.isLoading(), is(false));
    }

    @Test
    public void testIsLoadingLoading() {
        subject.setLoading(PATH_A);
        assertThat(subject.isLoading(), is(true));
    }

    @Test
    public void testIsLoadingLoaded() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        assertThat(subject.isLoading(), is(false));
    }

    @Test
    public void testIsLoadingLoadAgain() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        subject.setLoading(PATH_B);
        assertThat(subject.isLoading(), is(true));
    }

    @Test
    public void testIsLoadingPathConstructor() {
        assertThat(subject.isLoading(PATH_A), is(false));
    }

    @Test
    public void testIsLoadingPathLoading() {
        subject.setLoading(PATH_A);
        assertThat(subject.isLoading(PATH_A), is(true));
    }

    @Test
    public void testIsLoadingPathLoaded() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        assertThat(subject.isLoading(PATH_A), is(false));
    }

    @Test
    public void testIsLoadingPathLoadAgain() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        subject.setLoading(PATH_B);
        assertThat(subject.isLoading(PATH_B), is(true));
    }

    @Test
    public void testIsLoadingPathLoadAgainPreviousPath() {
        subject.setLoading(PATH_A);
        subject.setLoading(PATH_B);
        assertThat(subject.isLoading(PATH_A), is(false));
    }

    @Test
    public void testSetLoadingInit() {
        subject.setInit();
        subject.setLoading(PATH_A);
        assertThat(subject.isLoading(), is(true));
    }

    @Test
    public void testIsLoadedConstructor() {
        assertLoaded(false);
    }

    @Test
    public void testIsLoadedLoading() {
        subject.setLoading(PATH_A);
        assertLoaded(false);
    }

    @Test
    public void testIsLoadedLoaded() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        assertLoaded(true);
    }

    @Test
    public void testIsLoadedPathConstructor() {
        assertLoaded(PATH_A, false);
    }

    @Test
    public void testIsLoadedPathLoading() {
        subject.setLoading(PATH_A);
        assertLoaded(PATH_A, false);
    }

    @Test
    public void testIsLoadedPathLoaded() {
        subject.setLoading(PATH_A);
        subject.setLoaded();
        assertLoaded(PATH_A, true);
    }

    @Test
    public void testIsLoadedPathLoadAgain() {
        subject.setLoading(PATH_A);
        subject.setLoading(PATH_B);
        subject.setLoaded();
        assertLoaded(PATH_B, true);
    }

    @Test
    public void testIsLoadedPathLoadAgainPreviousPath() {
        subject.setLoading(PATH_A);
        subject.setLoading(PATH_B);
        subject.setLoaded();
        assertLoaded(PATH_A, false);
    }

    @Test
    public void testSetLoadedInit() {
        subject.setInit();
        subject.setLoaded();
        assertLoaded(true);
    }

    @Test
    public void testIsStoppedConstructor() {
        assertStopped(true);
    }

    @Test
    public void testIsStoppedPaused() {
        subject.setPaused();
        assertStopped(false);
    }

    @Test
    public void testIsStoppedDeinit() {
        subject.setPlaying();
        subject.setDeinit();
        assertStopped(true);
    }

    @Test
    public void testSetStopped() {
        subject.setPlaying();
        subject.setStopped();
        assertStopped(true);
    }

    @Test
    public void testIsPlayingConstructor() {
        assertPlaying(false);
    }

    @Test
    public void testIsPlayingPaused() {
        subject.setPaused();
        assertPlaying(false);
    }

    @Test
    public void testIsPlayingDeinit() {
        subject.setPlaying();
        subject.setDeinit();
        assertPlaying(false);
    }

    @Test
    public void testSetPlaying() {
        subject.setPlaying();
        assertPlaying(true);
    }

    @Test
    public void testIsPausedConstructor() {
        assertPaused(false);
    }

    @Test
    public void testIsPausedStopped() {
        subject.setStopped();
        assertPaused(false);
    }

    @Test
    public void testIsPausedDeinit() {
        subject.setPaused();
        subject.setDeinit();
        assertPaused(false);
    }

    @Test
    public void testSetPaused() {
        subject.setPaused();
        assertPaused(true);
    }

    private void assertInit(boolean init) {
        assertThat(subject.isInit(), is(init));
    }

    private void assertLoaded(boolean loaded) {
        assertThat(subject.isLoaded(), is(loaded));
    }

    private void assertLoaded(String path, boolean loaded) {
        assertThat(subject.isLoaded(path), is(loaded));
    }

    private void assertStopped(boolean stopped) {
        assertThat(subject.isStopped(), is(stopped));
    }

    private void assertPlaying(boolean playing) {
        assertThat(subject.isPlaying(), is(playing));
    }

    private void assertPaused(boolean paused) {
        assertThat(subject.isPaused(), is(paused));
    }

    private void assertPath(String path) {
        assertThat(subject.getPath(), is(path));
    }

    private void assertDeinit(boolean deinit) {
        assertThat(subject.isDeinit(), is(deinit));
    }
}
