package org.wikipedia.diff

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_article_edit_details.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.diff.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_ARTICLE_TITLE
import org.wikipedia.diff.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_EDIT_LANGUAGE_CODE
import org.wikipedia.diff.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_EDIT_REVISION_ID
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L


class ArticleEditDetailsFragment : Fragment() {
    private lateinit var articleTitle: String
    private var revisionId: Long = 0
    private var username: String? = null
    private var newerRevisionId: Long = 0
    private var olderRevisionId: Long = 0
    private lateinit var languageCode: String
    private val disposables = CompositeDisposable()
    private var menu: Menu? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_article_edit_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        articleTitle = StringUtils.defaultString(requireActivity().intent
                .getStringExtra(EXTRA_SOURCE_ARTICLE_TITLE), "")
        revisionId = requireActivity().intent
                .getLongExtra(EXTRA_SOURCE_EDIT_REVISION_ID, 0)
        languageCode = StringUtils.defaultString(requireActivity().intent
                .getStringExtra(EXTRA_SOURCE_EDIT_LANGUAGE_CODE), "en")
        articleTitleView.text = articleTitle
        newerButton.setOnClickListener {
            revisionId = newerRevisionId
            fetchNeighborEdits()
        }
        olderButton.setOnClickListener {
            revisionId = olderRevisionId
            fetchNeighborEdits()
        }
        thankButton.setOnClickListener { showThankDialog() }
        fetchNeighborEdits()
        highlightDiffText()
    }

    private fun showThankDialog() {
        val parent = FrameLayout(requireContext())
        val dialog: AlertDialog = AlertDialog.Builder(activity)
                .setView(parent)
                .setPositiveButton(R.string.thank_dialog_positive_button_text) { _, _ ->
                    sendThanks()
                }
                .setNegativeButton(R.string.thank_dialog_negative_button_text, null)
                .create()
        dialog.layoutInflater.inflate(R.layout.view_thank_dialog, parent)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.secondary_text_color))
        }
        dialog.show()
    }

    private fun sendThanks() {
        ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).csrfToken
                .subscribeOn(Schedulers.io())
                .flatMap { response: MwQueryResponse ->
                    val csrfToken = response.query()!!.csrfToken()
                    ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).postThanksToRevision(revisionId, csrfToken!!)
                }
                .subscribe({ FeedbackUtil.showMessage(activity, getString(R.string.thank_success_message, username)) })
                { Consumer { t: Throwable? -> L.e(t) } }
    }

    private fun showError() {
    }

    private fun fetchNeighborEdits() {
        fetchEditDetails()
        fetchEditDetails()
    }

    private fun updateUI(@NonNull currentRevision: Revision) {
        userIdButton.text = currentRevision.user
        editTimestamp.text = currentRevision.timeStamp()
        newerButton.isClickable = newerRevisionId.compareTo(-1) != 0
        olderButton.isClickable = olderRevisionId.compareTo(0) != 0
        ImageViewCompat.setImageTintList(newerButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (newerRevisionId.compareTo(-1) == 0) R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
        ImageViewCompat.setImageTintList(olderButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (olderRevisionId.compareTo(0) == 0) R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
        menu?.findItem(R.id.menu_user_talk_page)?.title = getString(R.string.menu_option_user_talk, currentRevision.user)
        menu?.findItem(R.id.menu_user_contributions_page)?.title = getString(R.string.menu_option_user_contributions, currentRevision.user)

    }

    private fun fetchEditDetails() {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getRevisionDetails(articleTitle, revisionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val currentRevision = response.query()!!.firstPage()!!.revisions()[0]
                    username = currentRevision.user
                    newerRevisionId = if (response.query()!!.firstPage()!!.revisions().size < 2) {
                        -1
                    } else {
                        response.query()!!.firstPage()!!.revisions()[1].revId
                    }
                    olderRevisionId = currentRevision.parentRevId

                    updateUI(currentRevision)
                }) { t: Throwable? -> L.e(t) })

    }

    private fun highlightDiffText() {
        val spannableString = SpannableString(diffText.text.toString())
        spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_57)), 0, diffText.text.length - 1, 0)
        diffText.text = spannableString
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                true
            }
            R.id.menu_user_talk_page -> {
                true
            }
            R.id.menu_user_contributions_page -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newInstance(): ArticleEditDetailsFragment {
            return ArticleEditDetailsFragment()
        }
    }
}
