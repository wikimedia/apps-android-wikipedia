package org.wikipedia.editactionfeed

import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions_item.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TITLE_DESC_SOURCE
import org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE
import org.wikipedia.editactionfeed.provider.MissingDescriptionProvider
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class AddTitleDescriptionsItemFragment : Fragment() {
    private val disposables = CompositeDisposable()
    private var summary: RbPageSummary? = null
    private val app = WikipediaApp.getInstance()
    private var sourceDescription: String = ""

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
        if (parent().source == EDIT_TASKS_TITLE_DESC_SOURCE) {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pageSummary ->
                        summary = pageSummary
                        updateContents()
                    }, { this.setErrorState(it) }))
        } else {
            disposables.add(MissingDescriptionProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(parent().langFromCode), parent().langToCode, true).subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.subscribe({ pair ->
                sourceDescription = StringUtils.defaultString(pair.first)
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

        if (parent().source == EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE) {
            val spannableDescription = SpannableString(sourceDescription)
            spannableDescription.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.text_highlight_color)), 0, sourceDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            viewArticleSubtitle.text = TextUtils.concat(String.format(getString(R.string.translation_source_description), app.language().getAppLanguageCanonicalName(parent().langFromCode)), spannableDescription)
        }
        viewArticleExtract.text = StringUtil.fromHtml(summary!!.extractHtml)
        viewArticleImage.loadImage(if (TextUtils.isEmpty(summary!!.thumbnailUrl)) null else Uri.parse(summary!!.thumbnailUrl))
    }

    private fun updateSourceDescriptionWithHighlight() {
        if (parent().source == EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE) {
            val spannableDescription = SpannableString(sourceDescription)
            spannableDescription.setSpan(ForegroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color)), 0, sourceDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            parent().sourceDescription = TextUtils.concat(String.format(getString(R.string.translation_source_description), app.language().getAppLanguageCanonicalName(parent().langFromCode)), spannableDescription)
        }
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
