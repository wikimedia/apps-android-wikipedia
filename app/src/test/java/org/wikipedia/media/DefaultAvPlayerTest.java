package org.wikipedia.media;

import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DefaultAvPlayerTest {
    private static final String PATH_A = "http://pathA";
    private static final String PATH_B = "http://pathB";

    private final FakeAvPlayerImplementation implementation = spy(new FakeAvPlayerImplementation());
    private AvPlayer subject = new DefaultAvPlayer(implementation);
    private final AvPlayer.Callback callback = mock(AvPlayer.Callback.class);
    private final AvPlayer.ErrorCallback errorCallback = mock(AvPlayer.ErrorCallback.class);

    @Test
    public void testDeinitDeinit() {
        subject.deinit();
        verify(implementation, never()).deinit();
    }

    @Test
    public void testDeinitInit() {
        subject.init();
        subject.deinit();
        verify(implementation).deinit();
    }

    @Test
    public void testDeinitLoad() {
        implementation.setAsyncLoadFailure(true);
        load(PATH_A);
        subject.deinit();
        verify(implementation).deinit();
    }

    @Test
    public void testInitConstructor() {
        verify(implementation, never()).init();
    }

    @Test
    public void testInitDeinit() {
        subject.deinit();
        subject.init();
        verify(implementation).init();
    }

    @Test
    public void testInitInit() {
        subject.init();
        subject.init();
        verify(implementation).init();
    }

    @Test
    public void testInitLoad() {
        load(PATH_A);
        verify(implementation).init();
    }

    @Test
    public void testInitStop() {
        subject.stop();
        verify(implementation, never()).init();
    }

    @Test
    public void testLoadLoad() {
        load(PATH_A);
        verifyLoad(1, PATH_A);
        verifyCallback(1);
        verifyErrorCallback(0);
    }

    @Test
    public void testLoadReloadSync() {
        load(PATH_A);
        load(PATH_B);
        verifyLoad(1, PATH_B);
        verifyCallback(2);
        verifyErrorCallback(0);
    }

    @Test
    public void testLoadReloadAsync() {
        implementation.setAsyncLoadFailure(true);
        load(PATH_A);
        implementation.setAsyncLoadFailure(false);
        load(PATH_B);
        verifyLoad(1, PATH_B);
        verifyCallback(1);
        verifyErrorCallback(0);
    }

    @Test
    public void testLoadFail() {
        implementation.setSyncLoadFailure(true);
        load(PATH_A);
        verifyLoad(1, PATH_A);
        verifyCallback(0);
        verifyErrorCallback(1);
    }

    @Test
    public void testLoadPlay() {
        play(PATH_A);
        verifyLoad(1, PATH_A);
        verifyCallback(1);
        verifyErrorCallback(0);
    }

    @Test
    public void testStopConstructor() {
        subject.stop();
        verify(implementation, never()).stop();
    }

    @Test
    public void testStopLoad() {
        implementation.setAsyncLoadFailure(true);
        load(PATH_A);
        subject.stop();
        verify(implementation, never()).stop();
        verifyCallback(0);
        verifyErrorCallback(0);
    }

    @Test
    public void testStopPlaySync() {
        play(PATH_A);
        subject.stop();
        verify(implementation, never()).stop();
        verifyCallback(1);
        verifyErrorCallback(0);
    }

    @Test
    public void testStopPlayAsync() {
        implementation.setAsyncPlayFailure(true);
        play(PATH_A);
        subject.stop();
        verify(implementation).stop();
        verifyCallback(0);
        verifyErrorCallback(0);
    }

    @Test
    public void testPlayPlaySync() {
        play(PATH_A);
        play(PATH_A);
        verifyPlay(2);
        verifyCallback(2);
        verifyErrorCallback(0);
    }

    @Test
    public void testPlayPlaySyncFail() {
        implementation.setSyncPlayFailure(true);
        play(PATH_A);
        verifyPlay(1);
        verifyCallback(0);
        verifyErrorCallback(1);
    }

    @Test
    public void testPlayLoadSyncFail() {
        implementation.setSyncLoadFailure(true);
        play(PATH_A);
        verifyPlay(0);
        verifyCallback(0);
        verifyErrorCallback(1);
    }

    @Test
    public void testPlayPlayAsync() {
        implementation.setAsyncPlayFailure(true);
        play(PATH_A);
        implementation.setAsyncPlayFailure(false);
        play(PATH_A);
        verifyPlay(1);
        verifyCallback(0);
        verifyErrorCallback(0);
    }

    private void load(String path) {
        subject.load(path, callback, errorCallback);
    }

    private void play(String path) {
        subject.play(path, callback, errorCallback);
    }

    private void verifyCallback(int times) {
        verify(callback, times(times)).onSuccess();
    }

    private void verifyErrorCallback(int times) {
        verify(errorCallback, times(times)).onError();
    }

    private void verifyLoad(int times, String path) {
        verify(implementation, times(times)).load(eq(path), anyCallback(), anyErrorCallback());
    }

    private void verifyPlay(int times) {
        verify(implementation, times(times)).play(anyCallback(), anyErrorCallback());
    }

    private AvPlayer.Callback anyCallback() {
        return any(AvPlayer.Callback.class);
    }

    private AvPlayer.ErrorCallback anyErrorCallback() {
        return any(AvPlayer.ErrorCallback.class);
    }
}
