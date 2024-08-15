package org.wikipedia.games.onthisday

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentOnThisDayGameFinalBinding
import org.wikipedia.databinding.ItemOnThisDayGameTopicBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
<<<<<<< HEAD
=======
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ReleaseUtil
>>>>>>> d39988b99c (Handle bookmark icon actions)
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale

class OnThisDayGameFinalFragment : Fragment(), WeeklyActivityView.Callback {
    private var _binding: FragmentOnThisDayGameFinalBinding? = null
    val binding get() = _binding!!

    private val viewModel: OnThisDayGameViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameFinalBinding.inflate(inflater, container, false)

        binding.shareButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .add(R.id.fragmentOverlayContainer, OnThisDayGameShareFragment.newInstance(viewModel.invokeSource))
                .addToBackStack(null)
                .commit()
        }

        viewModel.gameState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is Resource.Error -> updateOnError(it.throwable)
                is OnThisDayGameViewModel.GameEnded -> onGameEnded(it.data)
                else -> {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        is ArticleSavedOrDeletedEvent -> {
                            // TODO: work on this
                            binding.resultArticlesList.adapter?.notifyDataSetChanged()
                        }
                    }
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

    override fun onDayClick(date: LocalDate) {
        viewModel.resetCurrentDayState()
        requireActivity().finish()
        startActivity(OnThisDayGameActivity.newIntent(requireContext(), viewModel.invokeSource, date))
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

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState) {
        // TODO: moved to viewModel
        lifecycle.coroutineScope.launch {
            gameState.articles.forEach { pageSummary ->
                val inAnyList = AppDatabase.instance.readingListPageDao().findPageInAnyList(pageSummary.getPageTitle(WikipediaApp.instance.wikiSite)) != null
                if (inAnyList) {
                    viewModel.savedPages.add(pageSummary)
                }
            }
        }
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false
        binding.scrollContainer.isVisible = true
        val totalCorrect = gameState.answerState.count { it }
        binding.resultText.text = getString(R.string.on_this_day_game_result,
            totalCorrect,
            gameState.totalQuestions,
            getString(when (totalCorrect) {
                0 -> R.string.on_this_day_game_encourage0
                1 -> R.string.on_this_day_game_encourage1
                2 -> R.string.on_this_day_game_encourage2
                else -> R.string.on_this_day_game_encourage3
            }))

        val streak = calculateStreak(gameState.answerStateHistory)
        binding.streakText.text = StringUtil.fromHtml(resources.getQuantityString(R.plurals.on_this_day_game_streak, streak, streak))

        binding.resultArticlesList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultArticlesList.isNestedScrollingEnabled = false
        binding.resultArticlesList.adapter = RecyclerViewAdapter(gameState.articles)

        var displayStartDate = getStartOfWeekDate(OnThisDayGameViewModel.gameStartDate)
        while (displayStartDate.isBefore(OnThisDayGameViewModel.gameEndDate)) {
            val weekView = WeeklyActivityView(requireContext())
            weekView.callback = this
            binding.weeksContainer.addView(weekView)
            weekView.setWeekStats(displayStartDate, gameState)
            displayStartDate = displayStartDate.plusDays(7)
        }
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

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem(page: PageSummary, position: Int) {
            this.page = page
            binding.listItemTitle.text = StringUtil.fromHtml(page.displayTitle)
            binding.listItemDescription.text = StringUtil.fromHtml(page.description)
            binding.listItemDescription.isVisible = !page.description.isNullOrEmpty()
            binding.listItemBookmark.isVisible = true
            val isSaved = viewModel.savedPages.contains(page)
            binding.listItemBookmark.setOnClickListener {
                // TODO: handle saved state
                onBookmarkIconClick(it, page, position, isSaved)
            }
            val bookmarkResource = if (isSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
            val bookmarkTint = ResourceUtil.getThemedColorStateList(requireContext(), if (isSaved) R.attr.progressive_color else R.attr.secondary_color)
            binding.listItemBookmark.setImageResource(bookmarkResource)
            binding.listItemBookmark.imageTintList = bookmarkTint
            page.thumbnailUrl?.let {
                ViewUtil.loadImage(binding.listItemThumbnail, it, roundedCorners = true)
            }
        }

        override fun onClick(v: View) {
            val entry = HistoryEntry(page.getPageTitle(WikipediaApp.instance.wikiSite), HistoryEntry.SOURCE_PLACES)
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
        }
    }

    private fun onBookmarkIconClick(view: View, pageSummary: PageSummary, position: Int, isSaved: Boolean) {
        val pageTitle = pageSummary.getPageTitle(WikipediaApp.instance.wikiSite)
        if (isSaved) {
            LongPressMenu(view, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), pageTitle, addToDefault, InvokeSource.RANDOM_ACTIVITY) {
                        binding.resultArticlesList.adapter?.notifyItemChanged(position)
                    }
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(requireActivity(), page.listId, pageTitle, InvokeSource.RANDOM_ACTIVITY) {
                            binding.resultArticlesList.adapter?.notifyItemChanged(position)
                        }
                    }
                }
            }).show(HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_GAME))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), pageTitle, true, InvokeSource.RANDOM_ACTIVITY) {
                binding.resultArticlesList.adapter?.notifyItemChanged(position)
            }
        }
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameFinalFragment {
            return OnThisDayGameFinalFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }

        fun getStartOfWeekDate(date: LocalDate, locale: Locale = Locale.getDefault()): LocalDate {
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            val daysToSubtract = ((7 + (date.dayOfWeek.value - firstDayOfWeek.value)) % 7).toLong()
            return date.minusDays(daysToSubtract)
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

        fun timeUntilNextDay(): Duration {
            val now = LocalDateTime.now()
            val startOfNextDay = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
            return Duration.between(now, startOfNextDay)
        }
    }
}
