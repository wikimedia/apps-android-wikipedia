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
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel

class SingleWebViewActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleWebViewBinding
    private lateinit var blankLinkHandler: LinkHandler
    private lateinit var targetUrl: String
    val blankModel = PageViewModel()

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = ""

        targetUrl = intent.getStringExtra(EXTRA_URL)!!
        blankLinkHandler = EditLinkHandler(this, WikipediaApp.instance.wikiSite)

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

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, SingleWebViewActivity::class.java)
                    .putExtra(EXTRA_URL, url)
        }
    }
}
