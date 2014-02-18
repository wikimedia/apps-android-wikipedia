package org.wikipedia;

import android.os.*;
import android.util.*;
import android.webkit.*;
import org.json.*;

import java.util.*;

/**
 * Two way communications bridge between JS in a WebView and Java.
 */
public class CommunicationBridge {
    private final WebView webView;

    private final HashMap<String, ArrayList<JSEventListener>> eventListeners;

    private final BridgeMarshaller marshaller;

    private boolean isDOMReady = false;
    private final ArrayList<String> pendingJSMessages = new ArrayList<String>();

    public interface JSEventListener {
        public void onMessage(String messageType, JSONObject messagePayload);
    }

    public CommunicationBridge(final WebView webView, final String baseURL) {
        this.webView = webView;
        this.marshaller = new BridgeMarshaller();

        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebChromeClient(new CommunicatingChrome());
        webView.addJavascriptInterface(marshaller, "marshaller");

        webView.loadUrl(baseURL);

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

    public void clearAllListeners(String type) {
        eventListeners.remove(type);
    }

    public void sendMessage(String messageName, JSONObject messageData) {
        String messagePointer =  marshaller.putPayload(messageData.toString());

        StringBuilder jsString = new StringBuilder();
        jsString.append("javascript:handleMessage( ")
                .append("\"").append(messageName).append("\", \"")
                .append(messagePointer)
                .append("\" );");
        if (!isDOMReady) {
            pendingJSMessages.add(jsString.toString());
        } else {
            webView.loadUrl(jsString.toString());
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
            ArrayList<JSEventListener> listeners = eventListeners.get(type);
            for (JSEventListener listener : listeners) {
                listener.onMessage(type, messagePack.optJSONObject("payload"));
            }
            return false;
        }
    });

    private class CommunicatingChrome extends WebChromeClient {
        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            try {
                JSONObject messagePack = new JSONObject(message);
                Message msg = Message.obtain(incomingMessageHandler, MESSAGE_HANDLE_MESSAGE_FROM_JS, messagePack);
                incomingMessageHandler.sendMessage(msg);
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

    private static class BridgeMarshaller {
        private HashMap<String, String> queueItems = new HashMap<String, String>();
        private int counter = 0;

        /**
         * Called from the JS via the JSBridge to get actual payload from a messagePointer.
         *
         * Warning: This is going to be called on an indeterminable background thread, not main thread.
         *
         * @param pointer Key returned from #putPayload
         */
        @JavascriptInterface
        public String getPayload(String pointer) {
            synchronized (this) {
                return queueItems.remove(pointer);
            }
        }

        public String putPayload(String payload) {
            String key = "pointerKey_" + counter;
            counter++;
            synchronized (this) {
                queueItems.put(key, payload);
            }
            return key;
        }
    }
}
