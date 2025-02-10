package org.wikipedia.games.onthisday

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.FragmentOnThisDayGameFinalBinding
import org.wikipedia.databinding.ItemOnThisDayGameTopicBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.MarginItemDecoration
import org.wikipedia.views.ViewUtil
import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

class OnThisDayGameFinalFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameFinalBinding? = null
    val binding get() = _binding!!

    private val viewModel: OnThisDayGameViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameFinalBinding.inflate(inflater, container, false)

        WikiGamesEvent.submit("impression", "game_play", slideName = viewModel.getCurrentScreenName())

        binding.shareButton.setOnClickListener {
            WikiGamesEvent.submit("share_game_click", "game_play", slideName = viewModel.getCurrentScreenName())

            val shareMessage = getString(R.string.on_this_day_game_share_link_message,
                getString(R.string.on_this_day_game_share_url))
            ShareUtil.shareText(context = requireContext(), subject = "", text = shareMessage)
        }

        viewModel.gameState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is Resource.Error -> updateOnError(it.throwable)
                is OnThisDayGameViewModel.GameEnded -> onGameEnded(it.data, it.history)
                else -> {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

        timeUpdateRunnable = Runnable {
            val timeLeft = timeUntilNextDay()
            binding.nextGameText.text = getString(R.string.on_this_day_game_next_in, String.format(Locale.getDefault(), "%02d:%02d:%02d", timeLeft.toHoursPart(), timeLeft.toMinutesPart(), timeLeft.toSecondsPart()))
            handler.postDelayed(timeUpdateRunnable, 1000)
        }

        handler.post(timeUpdateRunnable)
        updateOnLoading()
        return binding.root
    }

    override fun onDestroyView() {
        handler.removeCallbacks(timeUpdateRunnable)
        _binding = null
        super.onDestroyView()
    }

    private fun updateOnLoading() {
        binding.errorView.isVisible = false
        binding.scrollContainer.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun updateOnError(t: Throwable) {
        binding.progressBar.isVisible = false
        binding.scrollContainer.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.setError(t)
    }

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState, history: OnThisDayGameViewModel.GameHistory) {
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false
        binding.scrollContainer.isVisible = true

        val totalCorrect = gameState.answerState.count { it }
        binding.resultText.text = getString(R.string.on_this_day_game_result, totalCorrect, gameState.totalQuestions)

        val cardContainerColor = when (totalCorrect) {
            0, 1, 2 -> R.color.yellow500
            3, 4 -> R.color.orange500
            else -> R.color.green600
        }
        binding.resultCardContainer.setBackgroundColor(ContextCompat.getColor(requireContext(), cardContainerColor))
        val totalGamesPlayed = calculateTotalGamesPlayed(history.history)
        binding.statsGamePlayed.text = String.format(totalGamesPlayed.toString())
        binding.statsGamePlayedText.text = resources.getQuantityString(R.plurals.on_this_day_game_stats_games_played, totalGamesPlayed)
        binding.statsAverageScore.text = DecimalFormat("0.#").format(calculateAverageScore(history.history))
        binding.statsCurrentStreak.text = String.format(calculateStreak(history.history).toString())

        binding.resultArticlesList.layoutManager = StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)
        binding.resultArticlesList.addItemDecoration(MarginItemDecoration(requireActivity(),
            R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_horizontal,
            R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_horizontal))
        binding.resultArticlesList.isNestedScrollingEnabled = false
        binding.resultArticlesList.adapter = RecyclerViewAdapter(viewModel.getArticlesMentioned())
    }

    private inner class RecyclerViewAdapter(val pages: List<PageSummary>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return pages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemOnThisDayGameTopicBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(pages[position], position)
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemOnThisDayGameTopicBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private lateinit var page: PageSummary
        private var position: Int = 0

        init {
            itemView.setOnClickListener(this)
            FeedbackUtil.setButtonTooltip(binding.listItemBookmark, binding.listItemShare)
        }

        fun bindItem(page: PageSummary, position: Int) {
            this.page = page
            this.position = position
            binding.listItemTitle.text = StringUtil.fromHtml(page.displayTitle)
            binding.listItemDescription.text = StringUtil.fromHtml(page.description)
            binding.listItemDescription.isVisible = !page.description.isNullOrEmpty()
            binding.listItemShare.setOnClickListener {
                WikiGamesEvent.submit("share_click", "game_play", slideName = viewModel.getCurrentScreenName())
                ShareUtil.shareText(requireActivity(), page.getPageTitle(WikipediaApp.instance.wikiSite))
            }
            val isSaved = updateBookmark()
            binding.listItemBookmark.isVisible = true
            binding.listItemBookmark.setOnClickListener {
                WikiGamesEvent.submit("save_click", "game_play", slideName = viewModel.getCurrentScreenName())
                onBookmarkIconClick(it, page, position, isSaved)
            }

            page.thumbnailUrl?.let {
                binding.listItemThumbnail.isVisible = true
                ViewUtil.loadImage(binding.listItemThumbnail, it, roundedCorners = true)
            } ?: run {
                binding.listItemThumbnail.isVisible = false
            }
        }

        private fun updateBookmark(): Boolean {
            val isSaved = viewModel.savedPages.contains(page)
            val bookmarkResource = if (isSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
            binding.listItemBookmark.setImageResource(bookmarkResource)
            return isSaved
        }

        override fun onClick(v: View) {
            WikiGamesEvent.submit("select_click", "game_play", slideName = viewModel.getCurrentScreenName())
            (requireActivity() as OnThisDayGameActivity).openArticleBottomSheet(page) { updateBookmark() }
        }
    }

    private fun onBookmarkIconClick(view: View, pageSummary: PageSummary, position: Int, isSaved: Boolean) {
        val pageTitle = pageSummary.getPageTitle(WikipediaApp.instance.wikiSite)
        if (isSaved) {
            LongPressMenu(view, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), pageTitle, addToDefault, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(requireActivity(), page.listId, pageTitle, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                    }
                }

                override fun onRemoveRequest() {
                    super.onRemoveRequest()
                    viewModel.savedPages.remove(pageSummary)
                    binding.resultArticlesList.adapter?.notifyItemChanged(position)
                }
            }).show(HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_GAME))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), pageTitle, true, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
            viewModel.savedPages.add(pageSummary)
            binding.resultArticlesList.adapter?.notifyItemChanged(position)
        }
    }

    companion object {
        fun newInstance(invokeSource: InvokeSource): OnThisDayGameFinalFragment {
            return OnThisDayGameFinalFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }

        fun calculateTotalGamesPlayed(answerStateHistory: Map<Int, Map<Int, Map<Int, List<Boolean>>>?>): Int {
            var total = 0
            answerStateHistory.forEach { year ->
                year.value?.forEach { month ->
                    month.value.forEach { day ->
                        total++
                    }
                }
            }
            return total
        }

        fun calculateStreak(answerStateHistory: Map<Int, Map<Int, Map<Int, List<Boolean>>>?>): Int {
            var streak = 0
            var date = LocalDate.now()
            while (answerStateHistory[date.year]?.get(date.monthValue)?.get(date.dayOfMonth) != null) {
                streak++
                date = date.minusDays(1)
            }
            return streak
        }

        fun calculateAverageScore(answerStateHistory: Map<Int, Map<Int, Map<Int, List<Boolean>>>?>): Float {
            var total = 0
            var count = 0
            answerStateHistory.forEach { year ->
                year.value?.forEach { month ->
                    month.value.forEach { day ->
                        total += day.value.count { it == true }
                        count++
                    }
                }
            }
            return total.toFloat() / count
        }

        fun timeUntilNextDay(): Duration {
            val now = LocalDateTime.now()
            val startOfNextDay = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
            return Duration.between(now, startOfNextDay)
        }
    }
}
