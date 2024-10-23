package org.wikipedia.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.ActivitySingleWebViewBinding
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class SingleWebViewActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleWebViewBinding
    private lateinit var blankLinkHandler: LinkHandler
    private lateinit var targetUrl: String
    private var currentUrl: String? = null
    private var pageTitleToLoadOnBackPress: PageTitle? = null
    private var showBackButton = false
    private var isWebForm = false
    private var wasFormSubmitted = false
    val blankModel = PageViewModel()

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        targetUrl = intent.getStringExtra(EXTRA_URL)!!
        showBackButton = intent.getBooleanExtra(EXTRA_SHOW_BACK_BUTTON, false)
        isWebForm = intent.getBooleanExtra(EXTRA_IS_WEB_FORM, false)
        pageTitleToLoadOnBackPress = intent.parcelableExtra(Constants.ARG_TITLE)
        blankLinkHandler = SingleWebViewLinkHandler(this, WikipediaApp.instance.wikiSite)

        binding.backButton.isVisible = showBackButton
        binding.backButton.setOnClickListener {
            goBack()
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.webViewClient = object : OkHttpWebViewClient() {
            override val model get() = blankModel
            override val linkHandler get() = blankLinkHandler

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (isWebForm && !request?.url.toString().startsWith(targetUrl)) {
                    finish()
                    request?.let {
                        // Special case: If the URL is the main page, then just allow the activity to close,
                        // otherwise, open the URL in an external browser.
                        if (!it.url.path.orEmpty().endsWith(StringUtil.addUnderscores(MainPageNameData.valueFor("en")))) {
                            UriUtil.visitInExternalBrowser(this@SingleWebViewActivity, it.url)
                        }
                    }
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.method == "POST") {
                    wasFormSubmitted = true
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.isVisible = true
                invalidateOptionsMenu()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.isVisible = false
                currentUrl = url
                view?.evaluateJavascript(JavaScriptActionHandler.mobileWebChromeShim(0, 0), null)
                invalidateOptionsMenu()
                if (isWebForm && wasFormSubmitted) {
                    binding.backButton.isVisible = true
                }

                // Explicitly apply any other cookies that we received during this call to the WebView,
                // so that subsequent POST(s) can inherit them.
                setCookies(url.orEmpty())
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                MaterialAlertDialogBuilder(this@SingleWebViewActivity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnDismissListener { result?.cancel() }
                    .show()
                return true
            }
        }

        // Explicitly apply our cookies to the default CookieManager of the WebView.
        // This is because our custom WebViewClient doesn't allow intercepting POST requests
        // properly, so in the case of POST requests the cookies will be supplied automatically.
        setCookies(targetUrl)

        if (savedInstanceState == null) {
            binding.webView.loadUrl(targetUrl)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_single_webview, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_group)?.isVisible = !binding.progressBar.isVisible && !currentUrl.isNullOrEmpty()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_in_new -> {
                currentUrl?.let {
                    UriUtil.visitInExternalBrowser(this@SingleWebViewActivity, Uri.parse(it))
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        binding.webView.clearAllListeners()
        (binding.webView.parent as ViewGroup).removeView(binding.webView)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!isWebForm && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return
        }
        goBack()
        super.onBackPressed()
    }

    private fun goBack() {
        if (intent.getStringExtra(EXTRA_PAGE_CONTENT_INFO).orEmpty() == PAGE_CONTENT_SOURCE_DONOR_EXPERIENCE) {
            DonorExperienceEvent.logAction("article_return_click", "webpay_processed")
        }
        pageTitleToLoadOnBackPress?.let {
            val entry = HistoryEntry(it, HistoryEntry.SOURCE_SINGLE_WEBVIEW)
            startActivity(PageActivity.newIntentForExistingTab(this@SingleWebViewActivity, entry, entry.title))
        }
        finish()
    }

    private fun setCookies(url: String) {
        CookieManager.getInstance().let {
            val cookies = SharedPreferenceCookieManager.instance.loadForRequest(url)
            for (cookie in cookies) {
                it.setCookie(url, cookie.toString())
            }
        }
    }

    inner class SingleWebViewLinkHandler(context: Context, override var wikiSite: WikiSite) : LinkHandler(context) {
        override fun onPageLinkClicked(anchor: String, linkText: String) { }
        override fun onInternalLinkClicked(title: PageTitle) { }
        override fun onMediaLinkClicked(title: PageTitle) { }
        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) { }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_SHOW_BACK_BUTTON = "goBack"
        const val EXTRA_PAGE_CONTENT_INFO = "pageContentInfo"
        const val PAGE_CONTENT_SOURCE_DONOR_EXPERIENCE = "donorExperience"
        const val EXTRA_IS_WEB_FORM = "isWebForm"

        fun newIntent(context: Context, url: String, showBackButton: Boolean = false, pageTitleToLoadOnBackPress: PageTitle? = null,
                      pageContentInfo: String? = null, isWebForm: Boolean = false): Intent {
            return Intent(context, SingleWebViewActivity::class.java)
                    .putExtra(EXTRA_URL, url)
                    .putExtra(EXTRA_SHOW_BACK_BUTTON, showBackButton)
                    .putExtra(Constants.ARG_TITLE, pageTitleToLoadOnBackPress)
                    .putExtra(EXTRA_PAGE_CONTENT_INFO, pageContentInfo)
                    .putExtra(EXTRA_IS_WEB_FORM, isWebForm)
        }
    }
}
