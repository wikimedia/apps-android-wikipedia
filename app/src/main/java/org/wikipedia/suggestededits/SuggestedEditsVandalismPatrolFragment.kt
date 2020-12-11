package org.wikipedia.suggestededits

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.*
import android.view.View.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.animation.ArgbEvaluatorCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_vandalism_item.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.*
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.log.L
import java.lang.Exception

class SuggestedEditsVandalismPatrolFragment : SuggestedEditsItemFragment() {
    var publishing: Boolean = false
    var publishSuccess: Boolean = false
    private var csrfClient: CsrfTokenClient = CsrfTokenClient(WikiSite(Service.COMMONS_URL))

    private var candidate: MwQueryResult.RecentChange? = null
    private var diff: DiffResponse? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_vandalism_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(contentContainer, parent().langFromCode)

        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.visibility = VISIBLE
            cardItemErrorView.visibility = GONE
            getNextItem()
        }

        publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.getInstance().currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        publishProgressBar.progressTintList = colorStateList
        publishProgressBarComplete.progressTintList = colorStateList
        publishProgressCheck.imageTintList = colorStateList
        publishProgressText.setTextColor(colorStateList)

        getNextItem()
        updateContents()

        oresGradient.background = GradientUtil.getPowerGradient(R.color.black26, Gravity.BOTTOM)

        voteGoodButton.setOnClickListener {
            // TODO
            parent().nextPage(this)
        }

        voteNotSureButton.setOnClickListener {
            // TODO
            parent().nextPage(this)
        }

        voteRevertButton.setOnClickListener {
            // TODO

            AlertDialog.Builder(requireContext())
                    .setMessage("TODO: revert right away, or take user to edit confirmation screen.")
                    .setPositiveButton(R.string.ok, null)
                    .show()

            //val title = PageTitle(candidate!!.title, WikiSite.forLanguageCode(parent().langFromCode))
            //UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(title.getUriForAction("history")))

            //parent().nextPage()
        }
    }

    override fun onStart() {
        super.onStart()
        parent().updateActionButton()
    }

    private fun getNextItem() {
        if (candidate != null) {
            return
        }
        disposables.add(EditingSuggestionsProvider.getNextRevertCandidate(parent().langFromCode)
                .flatMap {
                    candidate = it
                    ServiceFactory.getCoreRest(WikiSite.forLanguageCode(parent().langFromCode))
                            .getDiff(candidate!!.revFrom, candidate!!.revTo)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    diff = response
                    updateContents()
                }, { this.setErrorState(it) })!!)
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = VISIBLE
        cardItemProgressBar.visibility = GONE
        contentContainer.visibility = GONE
    }

    private fun updateContents() {
        cardItemErrorView.visibility = GONE
        contentContainer.visibility = if (candidate != null) VISIBLE else GONE
        cardItemProgressBar.visibility = if (candidate != null) GONE else VISIBLE
        if (candidate == null || diff == null) {
            return
        }


        val colorAdd = Color.rgb(220, 255, 220)
        val colorDelete = Color.rgb(255, 220, 220)


        if (candidate!!.ores != null) {
            oresScoreView.text = (candidate!!.ores!!.damagingProb * 100).toInt().toString() + "%"
            oresContainer.setBackgroundColor(ArgbEvaluatorCompat.getInstance().evaluate(candidate!!.ores!!.damagingProb, colorAdd, colorDelete))
            oresContainer.visibility = VISIBLE
        } else {
            oresContainer.visibility = GONE
        }
        articleTitleView.text = candidate!!.title

        val sb = SpannableStringBuilder()

        for (d in diff!!.diffs) {
            if (d.type == DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT) {
                sb.append(d.text)
                sb.append("\n")
            } else if (d.type == DiffResponse.DIFF_TYPE_LINE_ADDED) {
                val spanStart = sb.length
                sb.append(d.text)
                sb.setSpan(BackgroundColorSpan(colorAdd), spanStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("\n")
            } else if (d.type == DiffResponse.DIFF_TYPE_LINE_REMOVED) {
                val spanStart = sb.length
                sb.append(d.text)
                sb.setSpan(BackgroundColorSpan(colorDelete), spanStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("\n")
            } else if (d.type == DiffResponse.DIFF_TYPE_LINE_WITH_DIFF) {
                val spanStart = sb.length
                val indices = StringUtil.utf8Indices(d.text)
                sb.append(d.text)
                try {
                    for (range in d.highlightRanges) {
                        val rangeStart = indices[range.start]
                        val rangeEnd = if (range.start + range.length < indices.size) indices[range.start + range.length] else indices[indices.size - 1]

                        if (range.type == DiffResponse.HIGHLIGHT_TYPE_ADD) {
                            sb.setSpan(BackgroundColorSpan(colorAdd), spanStart + rangeStart, spanStart + rangeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else {
                            sb.setSpan(BackgroundColorSpan(colorDelete), spanStart + rangeStart, spanStart + rangeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            sb.setSpan(StrikethroughSpan(), spanStart + rangeStart, spanStart + rangeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                } catch (e: Exception) {
                    L.e(e)
                }
                sb.append("\n")
            }
        }

        diffTextView.setText(sb)

        /*
        ForegroundColorSpan(0xFFCC5500), start, longDescription.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        StyleSpan(android.graphics.Typeface.BOLD), start, longDescription.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
         */

        parent().updateActionButton()
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsVandalismPatrolFragment()
        }
    }

    override fun publish() {
        if (publishing || publishSuccess) {
            return
        }

        parent().nextPage(this)
    }

    private fun onSuccess() {
        val duration = 500L
        publishProgressBar.alpha = 1f
        publishProgressBar.animate()
                .alpha(0f)
                .duration = duration / 2

        publishProgressBarComplete.alpha = 0f
        publishProgressBarComplete.visibility = VISIBLE
        publishProgressBarComplete.animate()
                .alpha(1f)
                .withEndAction {
                    publishProgressText.setText(R.string.suggested_edits_image_tags_published)
                    playSuccessVibration()
                }
                .duration = duration / 2

        publishProgressCheck.alpha = 0f
        publishProgressCheck.visibility = VISIBLE
        publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        publishProgressBar.postDelayed({
            if (isAdded) {
                publishOverlayContainer.visibility = GONE
                parent().nextPage(this)
                setPublishedState()
            }
        }, duration * 3)
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        publishOverlayContainer.visibility = GONE
        FeedbackUtil.showError(requireActivity(), caught)
    }

    private fun setPublishedState() {
        // TODO
    }

    private fun playSuccessVibration() {
        suggestedEditsItemRootView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun publishOutlined(): Boolean {
        return true
    }

    override fun publishEnabled(): Boolean {
        return !publishSuccess
    }
}
