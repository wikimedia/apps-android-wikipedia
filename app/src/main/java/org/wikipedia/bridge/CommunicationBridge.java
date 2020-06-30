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
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.PageViewModel;
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

    private boolean isMetadataReady;
    private boolean isPcsReady;
    private final List<String> pendingJSMessages = new ArrayList<>();
    private final Map<String, ValueCallback<String>> pendingEvals = new HashMap<>();

    public interface JSEventListener {
        void onMessage(String messageType, JsonObject messagePayload);
    }

    public interface CommunicationBridgeListener {
        WebView getWebView();
        PageViewModel getModel();
        boolean isPreview();
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

    public void onPcsReady() {
        isPcsReady = true;
        flushMessages();
    }

    public void loadBlankPage() {
        communicationBridgeListener.getWebView().loadUrl("about:blank");
    }

    public void onMetadataReady() {
        isMetadataReady = true;
        flushMessages();
    }

    public boolean isLoading() {
        return !(isMetadataReady && isPcsReady);
    }

    public void resetHtml(@NonNull PageTitle pageTitle) {
        isPcsReady = false;
        isMetadataReady = false;
        pendingJSMessages.clear();
        pendingEvals.clear();
        if (communicationBridgeListener.getModel().shouldLoadAsMobileWeb()) {
            communicationBridgeListener.getWebView().loadUrl(pageTitle.getMobileUri());
        } else {
            communicationBridgeListener.getWebView().loadUrl(ServiceFactory.getRestBasePath(pageTitle.getWikiSite())
                    + RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.getPrefixedText()));
        }
    }

    public void cleanup() {
        pendingJSMessages.clear();
        pendingEvals.clear();
        eventListeners.clear();
        if (incomingMessageHandler != null) {
            incomingMessageHandler.removeCallbacksAndMessages(null);
            incomingMessageHandler = null;
        }
        communicationBridgeListener.getWebView().setWebViewClient(null);
        communicationBridgeListener.getWebView().removeJavascriptInterface("pcsClient");
        // Explicitly load a blank page into the WebView, to stop playback of any media.
        loadBlankPage();
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
        pendingJSMessages.add(jsString);
        flushMessages();
    }

    public void evaluate(@NonNull String js, ValueCallback<String> callback) {
        pendingEvals.put(js, callback);
        flushMessages();
    }

    public void evaluateImmediate(@NonNull String js, ValueCallback<String> callback) {
        communicationBridgeListener.getWebView().evaluateJavascript(js, callback);
    }

    private void flushMessages() {
        if (!isPcsReady || !isMetadataReady) {
            return;
        }
        for (String jsString : pendingJSMessages) {
            communicationBridgeListener.getWebView().loadUrl(jsString);
        }
        pendingJSMessages.clear();
        for (String key : pendingEvals.keySet()) {
            communicationBridgeListener.getWebView().evaluateJavascript(key, pendingEvals.get(key));
        }
        pendingEvals.clear();
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
            try {
                List<JSEventListener> listeners = eventListeners.get(message.getAction());
                for (JSEventListener listener : listeners) {
                    listener.onMessage(message.getAction(), message.getData());
                }
            } catch (Exception e) {
                e.printStackTrace();
                L.logRemoteError(e);
            }
            return false;
        }
    });

    private static class CommunicatingChrome extends WebChromeClient {
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
            return JavaScriptActionHandler.setUp(communicationBridgeListener.getWebView().getContext(),
                    communicationBridgeListener.getModel().getTitle(), communicationBridgeListener.isPreview());
        }
    }

    @SuppressWarnings("unused")
    private static class BridgeMessage {
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
