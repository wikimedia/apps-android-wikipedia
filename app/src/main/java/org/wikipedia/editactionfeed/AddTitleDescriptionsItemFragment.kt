package org.wikipedia.editactionfeed

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.*
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.editactionfeed.provider.MissingDescriptionProvider
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L



class AddTitleDescriptionsItemFragment : Fragment() {
    private val disposables = CompositeDisposable()
    private var summary: RbPageSummary? = null
    var pagerPosition = -1

    val title: PageTitle?
        get() = if (summary == null)
            null
        else
            PageTitle(summary!!.title, WikiSite.forLanguageCode(parent().langCode))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_add_title_descriptions_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewArticleImage.setLegacyVisibilityHandlingEnabled(true)
        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.visibility = View.VISIBLE
            getArticleWithMissingDescription()
        }
        updateContents()
        if (summary == null) {
            getArticleWithMissingDescription()
        }

        viewArticleContainer.setOnClickListener {
            if (title != null) {
                parent().onSelectPage(title!!)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun getArticleWithMissingDescription() {
        disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    summary = pageSummary
                    updateContents()
                },{ this.setErrorState(it) }))
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = View.VISIBLE
        cardItemProgressBar.visibility = View.GONE
        cardItemContainer.visibility = View.GONE
    }

    private fun updateContents() {
        cardItemErrorView.visibility = View.GONE
        cardItemContainer.visibility = if (summary == null) View.GONE else View.VISIBLE
        cardItemProgressBar.visibility = if (summary == null) View.VISIBLE else View.GONE
        if (summary == null) {
            return
        }
        viewArticleTitle.text = summary!!.normalizedTitle
        viewArticleSubtitle.text = null //summary.getDescription());
        viewArticleExtract.text = Html.fromHtml(summary!!.extractHtml)
        val observer = viewArticleExtract.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!isAdded || viewArticleExtract == null) {
                    return
                }
                val maxLines = viewArticleExtract.height / viewArticleExtract.lineHeight - 1
                val minLines = 3
                viewArticleExtract.maxLines = Math.max(maxLines, minLines)
                viewArticleExtract.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        viewArticleImage.loadImage(if (TextUtils.isEmpty(summary!!.thumbnailUrl)) null else Uri.parse(summary!!.thumbnailUrl))
    }

    private fun parent(): AddTitleDescriptionsFragment {
        return requireActivity().supportFragmentManager.fragments[0] as AddTitleDescriptionsFragment
    }

    companion object {

        fun newInstance(): AddTitleDescriptionsItemFragment {
            return AddTitleDescriptionsItemFragment()
        }
    }
}
