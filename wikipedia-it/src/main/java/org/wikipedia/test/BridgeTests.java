package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.webkit.WebView;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.CommunicationBridge;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BridgeTests extends ActivityUnitTestCase<TestDummyActivity> {
    public static final int TEST_COMPLETION_TIMEOUT = 2000;

    public BridgeTests() {
        super(TestDummyActivity.class);
    }

    private CommunicationBridge bridge;

    public void testDOMLoaded() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(), null, null);
                WebView webView = new WebView(getActivity());
                bridge = new CommunicationBridge(webView, "file:///android_asset/tests/index.html");
                bridge.addListener("DOMLoaded", new CommunicationBridge.JSEventListener() {
                    @Override
                    public JSONObject onMessage(String messageType, JSONObject messagePayload) {
                        assertEquals(messageType, "DOMLoaded");
                        completionLatch.countDown();
                        return null;
                    }
                });
            }
        });
        assertTrue(completionLatch.await(TEST_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testPingBackHandling() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(), null, null);
                WebView webView = new WebView(getActivity());
                bridge = new CommunicationBridge(webView, "file:///android_asset/tests/index.html");
                final JSONObject payload = new JSONObject();
                try {
                    payload.put("src", "./pingback");
                } catch (JSONException e) {
                    throw new RuntimeException(e); // JESUS CHRIST, JAVA!
                }
                bridge.sendMessage("ping", payload);
                bridge.addListener("pong", new CommunicationBridge.JSEventListener() {
                    @Override
                    public JSONObject onMessage(String messageType, JSONObject messagePayload) {
                        assertEquals(messageType, "pong");
                        assertEquals(messagePayload.toString(), payload.toString());
                        completionLatch.countDown();
                        return null;
                    }
                });
                bridge.sendMessage("injectScript", payload);
            }
        });
        assertTrue(completionLatch.await(TEST_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
