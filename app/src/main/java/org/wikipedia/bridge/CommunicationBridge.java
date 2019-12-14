package org.wikipedia.bridge;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.util.UriUtil;
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
    private final Map<String, List<JSEventListener>> eventListeners;
    private final CommunicationBridgeListener communicationBridgeListener;

    private boolean isDOMReady;
    private final List<String> pendingJSMessages = new ArrayList<>();

    public interface JSEventListener {
        void onMessage(String messageType, JsonObject messagePayload);
    }

    public interface CommunicationBridgeListener {
        WebView getWebView();
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    public CommunicationBridge(CommunicationBridgeListener communicationBridgeListener) {
        this.communicationBridgeListener = communicationBridgeListener;
        this.communicationBridgeListener.getWebView().getSettings().setJavaScriptEnabled(true);
        this.communicationBridgeListener.getWebView().getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.communicationBridgeListener.getWebView().getSettings().setMediaPlaybackRequiresUserGesture(false);
        this.communicationBridgeListener.getWebView().setWebChromeClient(new CommunicatingChrome());
        this.communicationBridgeListener.getWebView().addJavascriptInterface(new PcsClientJavascriptInterface(), "pcsClient");
        eventListeners = new HashMap<>();
    }

    public void onPageFinished() {
        isDOMReady = true;
        for (String jsString : pendingJSMessages) {
            communicationBridgeListener.getWebView().loadUrl(jsString);
        }
    }

    public void resetHtml(@NonNull String wikiUrl, String title) {
        isDOMReady = false;
        pendingJSMessages.clear();
        communicationBridgeListener.getWebView().loadUrl(wikiUrl + RestService.REST_API_PREFIX + RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(title));
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
            communicationBridgeListener.getWebView().loadUrl(jsString);
        }
    }

    public void evaluate(@NonNull String js, ValueCallback<String> callback) {
        communicationBridgeListener.getWebView().evaluateJavascript(js, callback);
    }

    private static final int MESSAGE_HANDLE_MESSAGE_FROM_JS = 1;
    private Handler incomingMessageHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            BridgeMessage message = (BridgeMessage) msg.obj;
            if (!eventListeners.containsKey(message.getAction())) {
                L.e("No such message type registered: " + message.getAction());
                return false;
            }
            List<JSEventListener> listeners = eventListeners.get(message.getAction());
            for (JSEventListener listener : listeners) {
                listener.onMessage(message.getAction(), message.getData());
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

    private class PcsClientJavascriptInterface {
        /**
         * Called from Javascript to send a message packet to the Java layer. The message must be
         * formatted in JSON, and URL-encoded.
         *
         * @param message JSON structured message received from the WebView.
         */
        @JavascriptInterface
        public synchronized void onReceiveMessage(String message) {
            if (incomingMessageHandler != null) {
                Message msg = Message.obtain(incomingMessageHandler, MESSAGE_HANDLE_MESSAGE_FROM_JS,
                        GsonUtil.getDefaultGson().fromJson(message, BridgeMessage.class));
                incomingMessageHandler.sendMessage(msg);
            }
        }

        @JavascriptInterface
        public synchronized String getSetupSettings() {
            return JavaScriptActionHandler.setUp();
        }
    }

    @SuppressWarnings("unused")
    private class BridgeMessage {
        @Nullable private String action;
        @Nullable private JsonObject data;

        @NonNull public String getAction() {
            return StringUtils.defaultString(action);
        }

        @Nullable public JsonObject getData() {
            return data;
        }
    }
}
