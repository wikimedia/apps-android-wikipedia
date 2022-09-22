package org.wikipedia.suggestededits

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.animation.ArgbEvaluatorCompat
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentSuggestedEditsVandalismItemBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.diff.DiffUtil
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.talk.TalkTopicActivity
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.usercontrib.UserContribListActivity
import org.wikipedia.util.*
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.log.L

class SuggestedEditsVandalismPatrolFragment : SuggestedEditsItemFragment() {
    private var _binding: FragmentSuggestedEditsVandalismItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SuggestedEditsVandalismPatrolViewModel by viewModels { SuggestedEditsVandalismPatrolViewModel.Factory(parent().langFromCode) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsVandalismItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(binding.contentContainer, parent().langFromCode)

        binding.diffRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.cardItemErrorView.backClickListener = OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = OnClickListener {
            binding.cardItemProgressBar.visibility = VISIBLE
            binding.cardItemErrorView.visibility = GONE
            viewModel.getCandidate()
        }

        val transparency = 0xcc000000
        binding.publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.instance.currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        binding.publishProgressBar.progressTintList = colorStateList
        binding.publishProgressBarComplete.progressTintList = colorStateList
        binding.publishProgressCheck.imageTintList = colorStateList
        binding.publishProgressText.setTextColor(colorStateList)

        binding.oresGradient.background = GradientUtil.getPowerGradient(R.color.black26, Gravity.BOTTOM)

        binding.voteGoodButton.setOnClickListener {
            parent().nextPage(this)
        }

        binding.voteNotSureButton.setOnClickListener {
            parent().nextPage(this)
        }

        binding.voteRevertButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Do you want to roll back this edit right now?")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    binding.publishProgressText.setText(R.string.suggested_edits_image_tags_publishing)
                    binding.publishProgressCheck.visibility = GONE
                    binding.publishOverlayContainer.visibility = VISIBLE
                    binding.publishProgressBarComplete.visibility = GONE
                    binding.publishProgressBar.visibility = VISIBLE
                    viewModel.doRollback()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.actionOverflowButton.setOnClickListener {
            PopupMenu(requireActivity(), binding.actionOverflowButton).run {
                inflate(R.menu.menu_vandalism_options)
                setOnMenuItemClickListener {
                    viewModel.candidate?.let { candidate ->
                        val title = PageTitle(candidate.title, viewModel.wikiSite)
                        when (it.itemId) {
                            R.id.menu_open_page -> {
                                val entry = HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK)
                                startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, title))
                            }
                            R.id.menu_edit_history -> {
                                startActivity(EditHistoryListActivity.newIntent(requireActivity(), title))
                            }
                            R.id.menu_user_talk_page -> {
                                startActivity(TalkTopicsActivity.newIntent(requireActivity(), PageTitle(UserTalkAliasData.valueFor(title.wikiSite.languageCode), candidate.user, viewModel.wikiSite),
                                        Constants.InvokeSource.PAGE_ACTIVITY))
                            }
                            R.id.menu_user_contribs -> {
                                startActivity(UserContribListActivity.newIntent(requireActivity(), candidate.user))
                            }
                        }
                    }
                    true
                }
                show()
            }
        }

        viewModel.candidateLiveData.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                updateContents(it.data.first, it.data.second)
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.rollbackResponse.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                onSuccess()
            } else if (it is Resource.Error) {
                onError(it.throwable)
            }
        }

        setProgressState()
    }

    override fun onStart() {
        super.onStart()
        parent().updateActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.contentContainer.visibility = GONE
    }

    private fun setProgressState() {
        binding.cardItemErrorView.visibility = GONE
        binding.contentContainer.visibility = GONE
        binding.cardItemProgressBar.visibility = VISIBLE
    }

    private fun updateContents(candidate: MwQueryResult.RecentChange, diff: DiffResponse) {
        binding.cardItemErrorView.visibility = GONE
        binding.contentContainer.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE

        val colorAdd = Color.rgb(220, 255, 220)
        val colorDelete = Color.rgb(255, 220, 220)

        if (candidate.ores != null) {
            binding.oresScoreView.text = (candidate.ores!!.damagingProb * 100).toInt().toString() + "%"
            binding.oresContainer.setBackgroundColor(ArgbEvaluatorCompat.getInstance().evaluate(candidate.ores!!.damagingProb, colorAdd, colorDelete))
            binding.oresContainer.visibility = VISIBLE
        } else {
            binding.oresContainer.visibility = GONE
        }
        binding.articleTitleView.text = candidate.title

        binding.userTextView.text = StringUtil.fromHtml("<b>User:</b> " + candidate.user)
        binding.summaryTextView.text = StringUtil.fromHtml("<b>Summary:</b> " + candidate.parsedcomment)

        binding.diffRecyclerView.adapter = DiffUtil.DiffLinesAdapter(DiffUtil.buildDiffLinesList(requireContext(), diff.diff))
        parent().updateActionButton()
    }

    override fun publish() {
        parent().nextPage(this)
    }

    private fun onSuccess() {
        val duration = 500L
        binding.publishProgressBar.alpha = 1f
        binding.publishProgressBar.animate()
                .alpha(0f)
                .duration = duration / 2

        binding.publishProgressBarComplete.alpha = 0f
        binding.publishProgressBarComplete.visibility = VISIBLE
        binding.publishProgressBarComplete.animate()
                .alpha(1f)
                .withEndAction {
                    binding.publishProgressText.setText(R.string.suggested_edits_image_tags_published)
                    playSuccessVibration()
                }
                .duration = duration / 2

        binding.publishProgressCheck.alpha = 0f
        binding.publishProgressCheck.visibility = VISIBLE
        binding.publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        binding.publishProgressBar.postDelayed({
            if (isAdded) {
                binding.publishOverlayContainer.visibility = GONE
                parent().nextPage(this)
                setPublishedState()
            }
        }, duration * 3)
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        binding.publishOverlayContainer.visibility = GONE
        FeedbackUtil.showError(requireActivity(), caught)
    }

    private fun setPublishedState() {
        // TODO
    }

    private fun playSuccessVibration() {
        binding.suggestedEditsItemRootView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun publishOutlined(): Boolean {
        return true
    }

    override fun publishEnabled(): Boolean {
        return true
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsVandalismPatrolFragment()
        }
    }
}
