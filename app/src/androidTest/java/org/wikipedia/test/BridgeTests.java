package org.wikipedia.test;

import android.support.test.runner.AndroidJUnit4;
import android.webkit.WebView;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
public class BridgeTests {
    private static final String TEST_FILE_URI = "file:///android_asset/tests/index.html";
    private static final String DOM_LOADED = "DOMLoaded";
    private static final String PING = "ping";
    private static final String PONG = "pong";
    private static final String SRC = "src";
    private static final String PINGBACK = "./pingback";
    private static final String INJECT_SCRIPT = "injectScript";

    private CommunicationBridge bridge;
    private WikipediaApp app = WikipediaApp.getInstance();
    private TestLatch completionLatch;
    private JSONObject payload;

    @Test
    public void testDOMLoaded() {
        completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebView webView = new WebView(app);
                bridge = new CommunicationBridge(webView, TEST_FILE_URI);
                bridge.addListener(DOM_LOADED, domLoadedListener);
            }
        });
        completionLatch.await();
    }

    private CommunicationBridge.JSEventListener domLoadedListener = new CommunicationBridge.JSEventListener() {
        @Override
        public void onMessage(String messageType, JSONObject messagePayload) {
            assertThat(messageType, equalTo(DOM_LOADED));
            completionLatch.countDown();
        }
    };

    @Test
    public void testPingBackHandling() {
        completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebView webView = new WebView(app);
                bridge = new CommunicationBridge(webView, TEST_FILE_URI);
                payload = new JSONObject();
                try {
                    payload.put(SRC, PINGBACK);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                bridge.sendMessage(PING, payload);
                bridge.addListener(PONG, pingBackListener);
                bridge.sendMessage(INJECT_SCRIPT, payload);
            }
        });
        completionLatch.await();
    }

    private CommunicationBridge.JSEventListener pingBackListener = new CommunicationBridge.JSEventListener() {
        @Override
        public void onMessage(String messageType, JSONObject messagePayload) {
            assertThat(messageType, equalTo(PONG));
            assertThat(messagePayload.toString(), equalTo(payload.toString()));
            completionLatch.countDown();
        }
    };

    private void runOnMainSync(Runnable r) {
        getInstrumentation().runOnMainSync(r);
    }
}
