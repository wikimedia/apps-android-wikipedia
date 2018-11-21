package org.wikipedia.bridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.ResourceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two-way communications bridge between JS in a WebView and Java.
 *
 * Messages TO the WebView are sent by calling loadUrl() with the Javascript payload in it.
 *
 * Messages FROM the WebView are received by leveraging @JavascriptInterface methods.
 *
 */
public class CommunicationBridge {
    private final WebView webView;

    private final Map<String, List<JSEventListener>> eventListeners;

    private final BridgeMarshaller marshaller;

    private boolean isDOMReady;
    private final List<String> pendingJSMessages = new ArrayList<>();

    public interface JSEventListener {
        void onMessage(String messageType, JSONObject messagePayload);
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    public CommunicationBridge(final WebView webView) {
        this.webView = webView;
        this.marshaller = new BridgeMarshaller();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.setWebChromeClient(new CommunicatingChrome());
        webView.addJavascriptInterface(marshaller, "marshaller");
        eventListeners = new HashMap<>();
        this.addListener("DOMLoaded", (messageType, messagePayload) -> {
            isDOMReady = true;
            for (String jsString : pendingJSMessages) {
                CommunicationBridge.this.webView.loadUrl(jsString);
            }
        });
    }

    public void resetHtml(@NonNull Context context, @NonNull String assetFileName, @NonNull String wikiUrl) {
        String html = "";
        try {
            final int rgbMask = 0xFFFFFF;
            html = FileUtil.readFile(WikipediaApp.getInstance().getAssets().open(assetFileName))
                    .replace("$wikiurl", wikiUrl)
                    .replace("$bodybackground", String.format("#%06X",
                            ResourceUtil.getThemedColor(context, R.attr.paper_color) & rgbMask));
        } catch (IOException e) {
            e.printStackTrace();
        }
        isDOMReady = false;
        webView.loadDataWithBaseURL(wikiUrl, html, "text/html", "utf-8", "");
    }

    public void cleanup() {
        eventListeners.clear();
        if (incomingMessageHandler != null) {
            incomingMessageHandler.removeCallbacksAndMessages(null);
            incomingMessageHandler = null;
        }
    }

    public void addListener(String type, JSEventListener listener) {
        if (eventListeners.containsKey(type)) {
            eventListeners.get(type).add(listener);
        } else {
            List<JSEventListener> listeners = new ArrayList<>();
            listeners.add(listener);
            eventListeners.put(type, listeners);
        }
    }

    public void sendMessage(String messageName, JSONObject messageData) {
        String messagePointer =  marshaller.putPayload(messageData.toString());

        String jsString = "javascript:handleMessage( \"" + messageName + "\", \"" + messagePointer + "\" );";
        if (!isDOMReady) {
            pendingJSMessages.add(jsString);
        } else {
            webView.loadUrl(jsString);
        }
    }

    private static final int MESSAGE_HANDLE_MESSAGE_FROM_JS = 1;
    private Handler incomingMessageHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            JSONObject messagePack = (JSONObject) msg.obj;
            String type = messagePack.optString("type");
            if (!eventListeners.containsKey(type)) {
                throw new RuntimeException("No such message type registered: " + type);
            }
            List<JSEventListener> listeners = eventListeners.get(type);
            for (JSEventListener listener : listeners) {
                listener.onMessage(type, messagePack.optJSONObject("payload"));
            }
            return false;
        }
    });

    private class CommunicatingChrome extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
            Log.d("WikipediaWeb", consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " - " + consoleMessage.message());
            return true;
        }
    }

    private class BridgeMarshaller {
        private Map<String, String> queueItems = new HashMap<>();
        private int counter = 0;

        /**
         * Called from the JS via the JSBridge to get actual payload from a messagePointer.
         *
         * Warning: This is going to be called on an indeterminable background thread, not main thread.
         *
         * @param pointer Key returned from #putPayload
         */
        @JavascriptInterface
        public synchronized String getPayload(String pointer) {
            return queueItems.remove(pointer);
        }

        public synchronized String putPayload(String payload) {
            String key = "pointerKey_" + counter;
            counter++;
            queueItems.put(key, payload);
            return key;
        }

        /**
         * Called from Javascript to send a message packet to the Java layer. The message must be
         * formatted in JSON, and URL-encoded.
         *
         * @param message JSON structured message received from the WebView.
         */
        @JavascriptInterface
        public synchronized void onReceiveMessage(String message) {
            try {
                if (incomingMessageHandler != null) {
                    JSONObject messagePack = new JSONObject(message);
                    Message msg = Message.obtain(incomingMessageHandler, MESSAGE_HANDLE_MESSAGE_FROM_JS, messagePack);
                    incomingMessageHandler.sendMessage(msg);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
