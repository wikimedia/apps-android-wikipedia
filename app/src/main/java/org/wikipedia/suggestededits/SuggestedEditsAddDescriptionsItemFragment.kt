package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants.InvokeSource.EDIT_FEED_TITLE_DESC
import org.wikipedia.Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class SuggestedEditsAddDescriptionsItemFragment : Fragment() {
    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    var sourceSummary: RbPageSummary? = null
    var targetSummary: RbPageSummary? = null
    var addedDescription: String = ""
        internal set
    var targetPageTitle: PageTitle? = null

    var pagerPosition = -1

    val title: String?
        get() = if (sourceSummary == null) null else sourceSummary!!.title

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
        if (sourceSummary == null) {
            getArticleWithMissingDescription()
        }

        cardView.setOnClickListener {
            if (sourceSummary != null) {
                parent().onSelectPage()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun getArticleWithMissingDescription() {
        if (parent().source == EDIT_FEED_TITLE_DESC) {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        sourceSummary = pageSummary
                        updateContents()
                    }, { this.setErrorState(it) }))
        } else {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode), parent().langToCode, true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        sourceSummary = pair.second
                        targetSummary = pair.first
                        targetPageTitle = targetSummary!!.getPageTitle(WikiSite.forLanguageCode(targetSummary!!.lang))
                        updateContents()
                    }, { this.setErrorState(it) })!!)
        }
    }

    fun showAddedDescriptionView(addedDescription: String?) {
        if (!TextUtils.isEmpty(addedDescription)) {
            viewArticleSubtitleContainer.visibility = VISIBLE
            if (parent().source == EDIT_FEED_TRANSLATE_TITLE_DESC) {
                viewArticleSubtitleAddedBy.visibility = VISIBLE
                viewArticleSubtitleEdit.visibility = VISIBLE
                viewArticleSubtitleAddedBy.text = getString(R.string.suggested_edits_translated_by_you)
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
        cardItemErrorView.visibility = VISIBLE
        cardItemProgressBar.visibility = GONE
        cardItemContainer.visibility = GONE
    }

    private fun updateContents() {
        cardItemErrorView.visibility = GONE
        cardItemContainer.visibility = if (sourceSummary == null) GONE else VISIBLE
        cardItemProgressBar.visibility = if (sourceSummary == null) VISIBLE else GONE
        if (sourceSummary == null) {
            return
        }
        viewArticleTitle.text = sourceSummary!!.normalizedTitle

        if (parent().source == EDIT_FEED_TRANSLATE_TITLE_DESC) {
            viewArticleSubtitleContainer.visibility = VISIBLE
            viewArticleSubtitleAddedBy.visibility = GONE
            viewArticleSubtitleEdit.visibility = GONE
            viewArticleSubtitle.text = StringUtils.capitalize(sourceSummary!!.description)
            callToActionText.text = String.format(getString(R.string.add_translation), app.language().getAppLanguageCanonicalName(parent().langToCode))
        }

        viewArticleExtract.text = StringUtil.fromHtml(sourceSummary!!.extractHtml)
        if (TextUtils.isEmpty(sourceSummary!!.thumbnailUrl)) {
            viewArticleImage.visibility = GONE
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = VISIBLE
            viewArticleImage.loadImage(Uri.parse(sourceSummary!!.thumbnailUrl))
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun parent(): SuggestedEditsAddDescriptionsFragment {
        return requireActivity().supportFragmentManager.fragments[0] as SuggestedEditsAddDescriptionsFragment
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 5
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 12

        fun newInstance(): SuggestedEditsAddDescriptionsItemFragment {
            return SuggestedEditsAddDescriptionsItemFragment()
        }
    }
}
