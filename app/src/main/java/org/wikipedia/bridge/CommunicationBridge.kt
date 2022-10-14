package org.wikipedia.bridge

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.wikipedia.bridge.JavaScriptActionHandler.setUp
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

/**
 * Two-way communications bridge between JS in a WebView and Java.
 *
 * Messages TO the WebView are sent by calling loadUrl() with the Javascript payload in it.
 *
 * Messages FROM the WebView are received by leveraging @JavascriptInterface methods.
 */
@SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
class CommunicationBridge constructor(private val communicationBridgeListener: CommunicationBridgeListener) {
    private val eventListeners = HashMap<String, MutableList<JSEventListener>>()
    private var isMetadataReady = false
    private var isPcsReady = false
    private val pendingJSMessages = ArrayList<String>()
    private val pendingEvals = HashMap<String, ValueCallback<String>>()

    fun interface JSEventListener {
        fun onMessage(messageType: String, messagePayload: JsonObject?)
    }

    interface CommunicationBridgeListener {
        val webView: WebView
        val model: PageViewModel
        val isPreview: Boolean
        val toolbarMargin: Int
    }

    init {
        communicationBridgeListener.webView.settings.javaScriptEnabled = true
        communicationBridgeListener.webView.settings.allowUniversalAccessFromFileURLs = true
        communicationBridgeListener.webView.settings.mediaPlaybackRequiresUserGesture = false
        communicationBridgeListener.webView.webChromeClient = CommunicatingChrome()
        communicationBridgeListener.webView.addJavascriptInterface(PcsClientJavascriptInterface(), "pcsClient")
    }

    fun onPcsReady() {
        isPcsReady = true
        flushMessages()
    }

    fun loadBlankPage() {
        communicationBridgeListener.webView.loadUrl("about:blank")
    }

    fun onMetadataReady() {
        isMetadataReady = true
        flushMessages()
    }

    val isLoading: Boolean
        get() = !(isMetadataReady && isPcsReady)

    fun resetHtml(pageTitle: PageTitle) {
        isPcsReady = false
        isMetadataReady = false
        pendingJSMessages.clear()
        pendingEvals.clear()
        if (communicationBridgeListener.model.shouldLoadAsMobileWeb) {
            communicationBridgeListener.webView.loadUrl(pageTitle.mobileUri)
        } else {
            communicationBridgeListener.webView.loadUrl(ServiceFactory.getRestBasePath(pageTitle.wikiSite) +
                    RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(pageTitle.prefixedText))
        }
    }

    fun cleanup() {
        pendingJSMessages.clear()
        pendingEvals.clear()
        eventListeners.clear()
        incomingMessageHandler?.removeCallbacksAndMessages(null)
        incomingMessageHandler = null
        communicationBridgeListener.webView.webViewClient = WebViewClient()
        communicationBridgeListener.webView.removeJavascriptInterface("pcsClient")
        // Explicitly load a blank page into the WebView, to stop playback of any media.
        loadBlankPage()
    }

    fun addListener(type: String, listener: JSEventListener) {
        eventListeners.getOrPut(type) { ArrayList() }.add(listener)
    }

    fun execute(js: String) {
        pendingJSMessages.add("javascript:$js")
        flushMessages()
    }

    fun evaluate(js: String, callback: ValueCallback<String>) {
        pendingEvals[js] = callback
        flushMessages()
    }

    fun evaluateImmediate(js: String, callback: ValueCallback<String?>?) {
        communicationBridgeListener.webView.evaluateJavascript(js, callback)
    }

    private fun flushMessages() {
        if (!isPcsReady || !isMetadataReady) {
            return
        }
        for (jsString in pendingJSMessages) {
            communicationBridgeListener.webView.loadUrl(jsString)
        }
        pendingJSMessages.clear()
        for ((key, callback) in pendingEvals) {
            communicationBridgeListener.webView.evaluateJavascript(key, callback)
        }
        pendingEvals.clear()
    }

    private var incomingMessageHandler: Handler? = Handler(Looper.getMainLooper(), Handler.Callback { msg ->
        val message = msg.obj as BridgeMessage
        if (!eventListeners.containsKey(message.action)) {
            L.e("No such message type registered: " + message.action)
            return@Callback false
        }
        try {
            val listeners: List<JSEventListener> = eventListeners[message.action]!!
            for (listener in listeners) {
                listener.onMessage(message.action, message.data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            L.logRemoteError(e)
        }
        false
    })

    private class CommunicatingChrome : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            L.d(consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " - " + consoleMessage.message())
            return true
        }
    }

    private inner class PcsClientJavascriptInterface {
        /**
         * Called from Javascript to send a message packet to the Java layer. The message must be
         * formatted in JSON, and URL-encoded.
         *
         * @param message JSON structured message received from the WebView.
         */
        @JavascriptInterface
        @Synchronized
        fun onReceiveMessage(message: String?) {
            if (incomingMessageHandler != null) {
                val bridgeMessage: BridgeMessage? = JsonUtil.decodeFromString(message.orEmpty())
                if (bridgeMessage != null) {
                    val msg = Message.obtain(incomingMessageHandler, MESSAGE_HANDLE_MESSAGE_FROM_JS, bridgeMessage)
                    incomingMessageHandler!!.sendMessage(msg)
                } else {
                    L.w("Received malformed message: $message")
                }
            }
        }

        @get:Synchronized
        @get:JavascriptInterface
        val setupSettings: String
            get() = setUp(communicationBridgeListener.webView.context,
                    communicationBridgeListener.model.title!!, communicationBridgeListener.isPreview,
                    communicationBridgeListener.toolbarMargin)
    }

    @Serializable
    private class BridgeMessage {
        val action: String = ""
        val data: JsonObject? = null
    }

    companion object {
        private const val MESSAGE_HANDLE_MESSAGE_FROM_JS = 1
    }
}
