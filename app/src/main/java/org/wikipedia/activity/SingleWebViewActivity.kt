package org.wikipedia.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.isVisible
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ActivitySingleWebViewBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel

class SingleWebViewActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleWebViewBinding
    private lateinit var blankLinkHandler: LinkHandler
    private lateinit var targetUrl: String
    private var pageTitle: PageTitle? = null
    private var showBackButton: Boolean = false
    val blankModel = PageViewModel()

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = ""

        targetUrl = intent.getStringExtra(EXTRA_URL)!!
        showBackButton = intent.getBooleanExtra(EXTRA_SHOW_BACK_BUTTON, false)
        pageTitle = intent.parcelableExtra(EXTRA_PAGE_TITLE)
        blankLinkHandler = EditLinkHandler(this, WikipediaApp.instance.wikiSite)

        binding.backButton.isVisible = showBackButton
        binding.backButton.setOnClickListener {
            pageTitle?.let {
                val entry = HistoryEntry(it, HistoryEntry.SOURCE_SINGLE_WEBVIEW)
                startActivity(PageActivity.newIntentForExistingTab(this@SingleWebViewActivity, entry, entry.title))
            }
            finish()
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.webViewClient = object : OkHttpWebViewClient() {
            override val model get() = blankModel
            override val linkHandler get() = blankLinkHandler

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.isVisible = false
            }
        }

        if (savedInstanceState == null) {
            binding.webView.loadUrl(targetUrl)
        }
    }

    override fun onDestroy() {
        binding.webView.clearAllListeners()
        (binding.webView.parent as ViewGroup).removeView(binding.webView)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
            return
        }
        super.onBackPressed()
    }

    inner class EditLinkHandler constructor(context: Context, override var wikiSite: WikiSite) : LinkHandler(context) {
        override fun onPageLinkClicked(anchor: String, linkText: String) { }
        override fun onInternalLinkClicked(title: PageTitle) { }
        override fun onMediaLinkClicked(title: PageTitle) { }
        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) { }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_SHOW_BACK_BUTTON = "goBack"
        const val EXTRA_PAGE_TITLE = "pageTitle"

        fun newIntent(context: Context, url: String, showBackButton: Boolean = false, pageTitle: PageTitle? = null): Intent {
            return Intent(context, SingleWebViewActivity::class.java)
                    .putExtra(EXTRA_URL, url)
                    .putExtra(EXTRA_SHOW_BACK_BUTTON, showBackButton)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
        }
    }
}
