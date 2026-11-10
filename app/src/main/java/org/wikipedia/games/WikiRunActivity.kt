package org.wikipedia.games

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.UriUtil
import java.io.File
import java.util.concurrent.Executors

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
        val request = JsonUtil.decodeFromString<BridgeRequest>(rawMessage) ?: return
        val requestId = request.id
        when (request.type) {
            "initialize" -> snapshotStore.read { snapshot ->
                reply(replyProxy, requestId, InitializeResult(snapshot, WikipediaApp.instance.currentTheme.isDark))
            }
            "saveSnapshot" -> {
                val snapshot = JsonUtil.decodeFromElement<WikiRunSnapshot>(request.payload)
                if (snapshot == null || snapshot.version != SNAPSHOT_VERSION) {
                    replyError(replyProxy, requestId, "Invalid WikiRun snapshot")
                    return
                }
                snapshotStore.save(snapshot) { saved ->
                    if (saved) reply(replyProxy, requestId, SaveSnapshotResult(saved = true))
                    else replyError(replyProxy, requestId, "Could not save WikiRun progress")
                }
            }
            "openLink" -> {
                val url = JsonUtil.decodeFromElement<UrlPayload>(request.payload)?.url.orEmpty()
                safeWebUri(url)?.let { UriUtil.visitInExternalBrowser(this, it) }
            }
            "openArticle" -> {
                val url = JsonUtil.decodeFromElement<UrlPayload>(request.payload)?.url.orEmpty()
                safeWebUri(url)?.let {
                    if (isArticleUri(it)) openArticle(it) else UriUtil.visitInExternalBrowser(this, it)
                }
            }
            "share" -> JsonUtil.decodeFromElement<SharePayload>(request.payload)?.let { share(it) }
            "close" -> finish()
            else -> if (requestId.isNotEmpty()) replyError(replyProxy, requestId, "Unsupported WikiRun bridge request")
        }
    }

    private inline fun <reified T> reply(replyProxy: JavaScriptReplyProxy, requestId: String, payload: T) {
        JsonUtil.encodeToString(ReplyEnvelope(requestId, JsonUtil.json.encodeToJsonElement(payload)))
            ?.let { replyProxy.postMessage(it) }
    }

    private fun replyError(replyProxy: JavaScriptReplyProxy, requestId: String, error: String) {
        JsonUtil.encodeToString(ErrorEnvelope(requestId, error))?.let { replyProxy.postMessage(it) }
    }

    private fun share(payload: SharePayload) {
        val title = payload.title ?: "WikiRun"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, listOfNotNull(
                payload.text?.takeIf { it.isNotBlank() },
                payload.url?.takeIf { it.isNotBlank() }
            ).joinToString("\n"))
        }
        startActivity(Intent.createChooser(sendIntent, title))
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

    @Serializable
    private class BridgeRequest(
        val id: String = "",
        val type: String = "",
        val payload: JsonElement? = null
    )

    @Serializable
    private class WikiRunSnapshot(
        val version: Int,
        val values: Map<String, String>
    )

    @Serializable
    private class InitializeResult(
        val snapshot: WikiRunSnapshot?,
        val darkMode: Boolean
    )

    @Serializable
    private class SaveSnapshotResult(val saved: Boolean)

    @Serializable
    private class UrlPayload(val url: String = "")

    @Serializable
    private class SharePayload(
        val title: String? = null,
        val text: String? = null,
        val url: String? = null
    )

    @Serializable
    private class ReplyEnvelope(val id: String, val payload: JsonElement)

    @Serializable
    private class ErrorEnvelope(val id: String, val error: String)

    private class SnapshotStore(private val file: File) {
        private val executor = Executors.newSingleThreadExecutor()

        fun read(onLoaded: (WikiRunSnapshot?) -> Unit) {
            execute {
                val snapshot = runCatching { file.readText() }.getOrNull()
                    ?.let { JsonUtil.decodeFromString<WikiRunSnapshot>(it) }
                Handler(Looper.getMainLooper()).post { onLoaded(snapshot) }
            }
        }

        fun save(snapshot: WikiRunSnapshot, onComplete: (Boolean) -> Unit) {
            execute {
                val saved = JsonUtil.encodeToString(snapshot)?.let { json ->
                    runCatching { file.writeText(json) }.isSuccess
                } ?: false
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
        private const val SNAPSHOT_VERSION = 1
    }
}
