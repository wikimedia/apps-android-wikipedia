package org.wikimedia.wikipedia;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Two way communications bridge between JS in a WebView and Java.
 */
public class CommunicationBridge {
    private final WebView webView;

    private final HashMap<String, ArrayList<JSEventListener>> eventListeners;

    private boolean isDOMReady = false;
    private final ArrayList<String> pendingJSMessages = new ArrayList<String>();

    public interface JSEventListener {
        public void onMessage(String messageType, JSONObject messagePayload);
    }

    public CommunicationBridge(final WebView webView) {
        this.webView = webView;
        webView.setWebChromeClient(new CommunicatingChrome());
        eventListeners = new HashMap<String, ArrayList<JSEventListener>>();
        this.addListener("DOMLoaded", new JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                isDOMReady = true;
                for(String jsString : pendingJSMessages) {
                    CommunicationBridge.this.webView.loadUrl(jsString);
                }
            }
        });
    }

    public void addListener(String type, JSEventListener listener) {
        if (eventListeners.containsKey(type)) {
            eventListeners.get(type).add(listener);
        } else {
            ArrayList<JSEventListener> listeners = new ArrayList<JSEventListener>();
            listeners.add(listener);
            eventListeners.put(type, listeners);
        }
    }

    public void sendMessage(String messageName, JSONObject messageData) {

        StringBuilder jsString = new StringBuilder();
        jsString.append("javascript:bridge.handleMessage( ")
                .append("\"").append(messageName).append("\",")
                .append(messageData.toString())
                .append(" );");
        if (!isDOMReady) {
            pendingJSMessages.add(jsString.toString());
        } else {
            webView.loadUrl(jsString.toString());
        }
    }

    private class CommunicatingChrome extends WebChromeClient {
        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            try {
                JSONObject messagePack = new JSONObject(message);
                String type = messagePack.getString("type");
                if (!eventListeners.containsKey(type)) {
                    throw new RuntimeException("No such message type registered: " + type);
                }
                ArrayList<JSEventListener> listeners = eventListeners.get(type);
                for (JSEventListener listener : listeners) {
                    listener.onMessage(type, messagePack.getJSONObject("payload"));
                }
                result.confirm();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d("WikipediaWeb", consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " - " + consoleMessage.message());
            return true;
        }
    }
}
