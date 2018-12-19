package org.wikipedia.editactionfeed

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity.Companion.MULTILINGUAL_DESC
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity.Companion.TITLE_DESC
import org.wikipedia.editactionfeed.provider.MissingDescriptionProvider
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L


class AddTitleDescriptionsItemFragment : Fragment() {
    private val disposables = CompositeDisposable()
    private var summary: RbPageSummary? = null
    private val app = WikipediaApp.getInstance()
    var sourceDescription: String? = null

    var pagerPosition = -1

    val title: PageTitle?
        get() = if (summary == null)
            null
        else
            PageTitle(summary!!.title, WikiSite.forLanguageCode(parent().langToCode))

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
        if (parent().source == TITLE_DESC) {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        summary = pageSummary
                        updateContents()
                    }, { this.setErrorState(it) }))
        } else {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode), parent().langToCode, true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        sourceDescription = pair.first.description
                        if (pagerPosition == 0) {
                            parent().sourceDescription = StringUtils.join(String.format(getString(R.string.translation_source_description), app.language().getAppLanguageCanonicalName(parent().langFromCode)), sourceDescription)
                        }
                        summary = pair.second
                        updateContents()
                    }, { this.setErrorState(it) })!!)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isAdded && isVisibleToUser) {
            parent().sourceDescription = StringUtils.join(String.format(getString(R.string.translation_source_description), app.language().getAppLanguageCanonicalName(parent().langFromCode)), sourceDescription)
        }
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

        if (parent().source == MULTILINGUAL_DESC) {
            val spannableDescription = SpannableString(sourceDescription)
            spannableDescription.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.text_highlight_color)), 0, sourceDescription!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            viewArticleSubtitle.text = TextUtils.concat(String.format(getString(R.string.translation_source_description), app.language().getAppLanguageCanonicalName(parent().langFromCode)), spannableDescription)
        }

        viewArticleExtract.text = StringUtil.fromHtml(summary!!.extractHtml)
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
