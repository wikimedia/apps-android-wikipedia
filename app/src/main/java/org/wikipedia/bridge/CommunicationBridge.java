package org.wikipedia.bridge;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.util.log.L;

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

    private boolean isDOMReady;
    private final List<String> pendingJSMessages = new ArrayList<>();

    public interface JSEventListener {
        void onMessage(String messageType, JSONObject messagePayload);
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    public CommunicationBridge(final WebView webView) {
        this.webView = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new CommunicatingChrome());
        webView.addJavascriptInterface(new BridgeMarshaller(), "marshaller");
        eventListeners = new HashMap<>();
    }

    public void onPageFinished() {
        isDOMReady = true;
        for (String jsString : pendingJSMessages) {
            webView.loadUrl(jsString);
        }
    }

    public void resetHtml(@NonNull String wikiUrl, String title) {
        isDOMReady = false;
        pendingJSMessages.clear();
        webView.loadUrl(wikiUrl + "/" + RestService.REST_API_PREFIX + RestService.PAGE_HTML_ENDPOINT + title);
        execute(JavaScriptActionHandler.setHandler());
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

    public void execute(@NonNull String js) {
        String jsString = "javascript:" + js;
        if (!isDOMReady) {
            pendingJSMessages.add(jsString);
        } else {
            webView.loadUrl(jsString);
        }
    }

    @Deprecated
    public void sendMessage(String messageName, JSONObject messageData) {
        // TODO: remove/convert all remaining places where we call this method.
    }

    private static final int MESSAGE_HANDLE_MESSAGE_FROM_JS = 1;
    private Handler incomingMessageHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            JSONObject messagePack = (JSONObject) msg.obj;
            String type = messagePack.optString("action");
            if (!eventListeners.containsKey(type)) {
                throw new RuntimeException("No such message type registered: " + type);
            }
            List<JSEventListener> listeners = eventListeners.get(type);
            for (JSEventListener listener : listeners) {
                listener.onMessage(type, messagePack.optJSONObject("data"));
            }
            return false;
        }
    });

    private class CommunicatingChrome extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
            L.d(consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " - " + consoleMessage.message());
            return true;
        }
    }

    private class BridgeMarshaller {
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
