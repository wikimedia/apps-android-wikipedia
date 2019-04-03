package org.wikipedia.editactionfeed

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.editactionfeed.provider.MissingDescriptionProvider
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class AddTitleDescriptionsItemFragment : Fragment() {
    private val disposables = CompositeDisposable()
    private var summary: RbPageSummary? = null
    private val app = WikipediaApp.getInstance()
    var sourceDescription: String = ""
    var addedDescription: String = ""
        internal set
    var targetPageTitle: PageTitle? = null

    var pagerPosition = -1

    val title: String?
        get() = if (summary == null) null else summary!!.title

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
        setConditionalLayoutDirection(viewArticleContainer, parent().langFromCode)
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

        cardView.setOnClickListener {
            if (summary != null) {
                parent().onSelectPage()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun getArticleWithMissingDescription() {
        if (parent().source == InvokeSource.EDIT_FEED_TITLE_DESC) {
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
                        targetPageTitle = pair.first
                        sourceDescription = StringUtils.defaultString(pair.second.description)

                        if (pagerPosition == 0) {
                            updateSourceDescriptionWithHighlight()
                        }
                        summary = pair.second
                        updateContents()
                    }, { this.setErrorState(it) })!!)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isAdded && isVisibleToUser) {
            updateSourceDescriptionWithHighlight()
        }
    }

    fun showAddedDescriptionView(addedDescription: String?) {
        if (!TextUtils.isEmpty(addedDescription)) {
            viewArticleSubtitleContainer.visibility = VISIBLE
            if (parent().source == InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
                viewArticleSubtitleAddedBy.visibility = VISIBLE
                viewArticleSubtitleEdit.visibility = VISIBLE
                viewArticleSubtitleAddedBy.text = getString(R.string.editactionfeed_translated_by_you)
            }
            viewAddDescriptionButton.visibility = GONE
            viewArticleSubtitle.text = addedDescription
            viewArticleExtract.maxLines = viewArticleExtract.maxLines - 1
            this.addedDescription = addedDescription!!
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

        if (parent().source == InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            viewArticleSubtitleContainer.visibility = VISIBLE
            viewArticleSubtitleAddedBy.visibility = GONE
            viewArticleSubtitleEdit.visibility = GONE
            viewArticleSubtitle.text = sourceDescription
            callToActionText.text = String.format(getString(R.string.add_translation), app.language().getAppLanguageCanonicalName(parent().langToCode))
        }

        viewArticleExtract.text = StringUtil.fromHtml(summary!!.extractHtml)
        if (TextUtils.isEmpty(summary!!.thumbnailUrl)) {
            viewArticleImage.visibility = GONE
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = VISIBLE
            viewArticleImage.loadImage(Uri.parse(summary!!.thumbnailUrl))
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun updateSourceDescriptionWithHighlight() {
        if (parent().source == InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            val spannableDescription = SpannableString(sourceDescription)
            spannableDescription.setSpan(ForegroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color)), 0, sourceDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun parent(): AddTitleDescriptionsFragment {
        return requireActivity().supportFragmentManager.fragments[0] as AddTitleDescriptionsFragment
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 6
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 13

        fun newInstance(): AddTitleDescriptionsItemFragment {
            return AddTitleDescriptionsItemFragment()
        }
    }
}
