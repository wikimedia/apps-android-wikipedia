package org.wikipedia.games

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.UriUtil
import java.io.File
import java.util.concurrent.Executors
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class WikiRunActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var snapshotStore: SnapshotStore
    private lateinit var gameUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameUri = Uri.parse(BuildConfig.WIKIRUN_BASE_URL)
            .buildUpon()
            .appendQueryParameter("embed", "1")
            .build()
        snapshotStore = SnapshotStore(File(noBackupFilesDir, "wikirun-state.json"))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (isSameOrigin(uri)) return false
                    if (uri.scheme == "https" || uri.scheme == "http") {
                        UriUtil.visitInExternalBrowser(this@WikiRunActivity, uri)
                    }
                    return true
                }
            }
        }

        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            finish()
            return
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                "window.__wikiRunEmbed = true;",
                setOf(originRule(gameUri))
            )
        }
        WebViewCompat.addWebMessageListener(
            webView,
            BRIDGE_NAME,
            setOf(originRule(gameUri))
        ) { _, message, sourceOrigin, isMainFrame, replyProxy ->
            if (isMainFrame && isSameOrigin(sourceOrigin)) handleBridgeMessage(message, replyProxy)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "window.__wikiRunHandleBack ? window.__wikiRunHandleBack() : null"
                ) { result ->
                    when (result) {
                        "true" -> Unit
                        "false" -> finish()
                        // The game did not expose its back handler, fall back to WebView history.
                        else -> if (webView.canGoBack()) webView.goBack() else finish()
                    }
                }
            }
        })
        webView.loadUrl(gameUri.toString())
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            WebViewCompat.removeWebMessageListener(webView, BRIDGE_NAME)
            webView.destroy()
        }
        if (::snapshotStore.isInitialized) snapshotStore.close()
        super.onDestroy()
    }

    private fun handleBridgeMessage(message: WebMessageCompat, replyProxy: JavaScriptReplyProxy) {
        val rawMessage = message.data ?: return
        val envelope = runCatching { JSONObject(rawMessage) }.getOrNull() ?: return
        val requestId = envelope.optString("id")
        val type = envelope.optString("type")
        val payload = envelope.optJSONObject("payload") ?: JSONObject()
        when (type) {
            "initialize" -> snapshotStore.read { snapshot ->
                val response = JSONObject()
                    .put("snapshot", snapshot ?: JSONObject.NULL)
                    .put("darkMode", WikipediaApp.instance.currentTheme.isDark)
                reply(replyProxy, requestId, response)
            }
            "saveSnapshot" -> {
                if (payload.optInt("version") != 1) {
                    replyError(replyProxy, requestId, "Invalid WikiRun snapshot")
                    return
                }
                val values = payload.optJSONObject("values") ?: JSONObject()
                snapshotStore.save(JSONObject().put("version", 1).put("values", values)) { saved ->
                    if (saved) reply(replyProxy, requestId, JSONObject().put("saved", true))
                    else replyError(replyProxy, requestId, "Could not save WikiRun progress")
                }
            }
            "openLink" -> {
                safeWebUri(payload.optString("url"))?.let { UriUtil.visitInExternalBrowser(this, it) }
            }
            "openArticle" -> {
                safeWebUri(payload.optString("url"))?.let {
                    if (isArticleUri(it)) openArticle(it) else UriUtil.visitInExternalBrowser(this, it)
                }
            }
            "share" -> share(payload)
            "close" -> finish()
            else -> if (requestId.isNotEmpty()) replyError(replyProxy, requestId, "Unsupported WikiRun bridge request")
        }
    }

    private fun reply(replyProxy: JavaScriptReplyProxy, requestId: String, payload: JSONObject) {
        replyProxy.postMessage(JSONObject().put("id", requestId).put("payload", payload).toString())
    }

    private fun replyError(replyProxy: JavaScriptReplyProxy, requestId: String, error: String) {
        replyProxy.postMessage(JSONObject().put("id", requestId).put("error", error).toString())
    }

    private fun share(payload: JSONObject) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, payload.optString("title", "WikiRun"))
            putExtra(Intent.EXTRA_TEXT, listOfNotNull(
                payload.optString("text").takeIf { it.isNotBlank() },
                payload.optString("url").takeIf { it.isNotBlank() }
            ).joinToString("\n"))
        }
        startActivity(Intent.createChooser(sendIntent, payload.optString("title", "WikiRun")))
    }

    private fun safeWebUri(value: String): Uri? {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        return uri.takeIf { it.scheme == "http" || it.scheme == "https" }
    }

    private fun isArticleUri(uri: Uri): Boolean =
        (uri.scheme == "https" || uri.scheme == "http") && UriUtil.isAppSupportedLink(uri)

    /**
     * Opens an article the game asked for by message, so it is the game that decides what is
     * encyclopedic content; anchors rendered inside the game keep going to the external browser.
     * The game stays in the back stack, so back returns to the game-over screen. Source 43 is the
     * shared "game" source, matching `ArticleSource.game` in the iOS app.
     */
    private fun openArticle(uri: Uri) {
        val title = PageTitle.titleForUri(uri, WikiSite(uri))
        val entry = HistoryEntry(title, HistoryEntry.SOURCE_ON_THIS_DAY_GAME)
        startActivity(PageActivity.newIntentForNewTab(this, entry, title))
    }

    private fun isSameOrigin(uri: Uri): Boolean =
        uri.scheme == gameUri.scheme && uri.host == gameUri.host && uri.port == gameUri.port

    private fun originRule(uri: Uri): String = "${uri.scheme}://${uri.host}${if (uri.port >= 0) ":${uri.port}" else ""}"

    private class SnapshotStore(private val file: File) {
        private val executor = Executors.newSingleThreadExecutor()

        fun read(onLoaded: (JSONObject?) -> Unit) {
            execute {
                val snapshot = runCatching { JSONObject(file.readText()) }.getOrNull()
                Handler(Looper.getMainLooper()).post { onLoaded(snapshot) }
            }
        }

        fun save(snapshot: JSONObject, onComplete: (Boolean) -> Unit) {
            execute {
                val saved = runCatching { file.writeText(snapshot.toString()) }.isSuccess
                Handler(Looper.getMainLooper()).post { onComplete(saved) }
            }
        }

        fun close() {
            executor.shutdown()
        }

        private fun execute(task: () -> Unit) {
            runCatching { executor.execute(task) }
        }
    }

    companion object {
        private const val BRIDGE_NAME = "WikiRunBridge"
    }
}
