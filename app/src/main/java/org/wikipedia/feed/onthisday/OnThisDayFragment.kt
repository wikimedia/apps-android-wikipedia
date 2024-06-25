package org.wikipedia.feed.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.databinding.FragmentOnThisDayBinding
import org.wikipedia.databinding.ViewEventsLayoutBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CustomDatePicker
import org.wikipedia.views.HeaderMarginItemDecoration
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class OnThisDayFragment : Fragment(), CustomDatePicker.Callback {

    private var _binding: FragmentOnThisDayBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayViewModel by viewModels { OnThisDayViewModel.Factory(requireArguments()) }
    private var positionToScrollTo = 0
    private val appCompatActivity get() = requireActivity() as AppCompatActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnThisDayBinding.inflate(inflater, container, false)
        binding.eventsRecycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.eventsRecycler.addItemDecoration(HeaderMarginItemDecoration(24, 0))
        binding.eventsRecycler.isNestedScrollingEnabled = true
        binding.eventsRecycler.clipToPadding = false
        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.dayContainer.setOnClickListener { onCalendarClicked() }
        binding.toolbarDayContainer.setOnClickListener { onCalendarClicked() }
        binding.indicatorLayout.setOnClickListener { onIndicatorLayoutClicked() }
        setUpToolbar()
        setUpTransitionAnimation(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> onSuccess(it.data)
                            is Resource.Error -> onError(it.throwable)
                        }
                    }
                }
            }
        }
    }

    private fun onLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.eventsRecycler.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun setUpTransitionAnimation(savedInstanceState: Bundle?) {
        val animDelay = if (requireActivity().window.sharedElementEnterTransition != null &&
            savedInstanceState == null) 500L else 0L
        binding.onThisDayTitleView.postDelayed({
            if (!isAdded) {
                return@postDelayed
            }
            viewModel.loadOnThisDay()
        }, animDelay)
    }

    private fun onSuccess(events: List<OnThisDay.Event>) {
        binding.progressBar.visibility = View.GONE
        binding.eventsRecycler.visibility = View.VISIBLE
        binding.errorView.visibility = View.GONE
        binding.eventsRecycler.adapter = RecyclerAdapter(events, viewModel.wikiSite)
        positionToScrollTo = events.indices.find { viewModel.year == events[it].year } ?: 0
        binding.dayInfo.text = getString(R.string.events_count_text, events.size.toString(),
            DateUtil.yearToStringWithEra(events.last().year), events[0].year)
        binding.eventsRecycler.postDelayed({
            if (isAdded && positionToScrollTo != -1 && viewModel.year != -1) {
                binding.eventsRecycler.scrollToPosition(positionToScrollTo)
            }
        }, 500)
    }

    private fun onError(throwable: Throwable) {
        L.e(throwable)
        binding.progressBar.visibility = View.GONE
        binding.errorView.setError(throwable)
        binding.errorView.visibility = View.VISIBLE
        binding.eventsRecycler.visibility = View.GONE
    }

    private fun setUpToolbar() {
        appCompatActivity.setSupportActionBar(binding.toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        appCompatActivity.supportActionBar?.title = ""
        binding.collapsingToolbarLayout.setCollapsedTitleTextColor(
            ResourceUtil.getThemedColor(requireContext(), R.attr.primary_color)
        )
        binding.day.text = DateUtil.getMonthOnlyDateString(viewModel.date.time)
        maybeHideDateIndicator()
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.headerFrameLayout.alpha = 1.0f - abs(verticalOffset / appBarLayout.totalScrollRange.toFloat())
            if (verticalOffset > -appBarLayout.totalScrollRange) {
                binding.dropDownToolbar.visibility = View.GONE
            } else if (verticalOffset <= -appBarLayout.totalScrollRange) {
                binding.dropDownToolbar.visibility = View.VISIBLE
            }
            val newText = if (verticalOffset <= -appBarLayout.totalScrollRange) DateUtil.getMonthOnlyDateString(viewModel.date.time) else ""
            if (newText != binding.toolbarDay.text.toString()) {
                appBarLayout.post { binding.toolbarDay.text = newText }
            }
        }
    }

    private fun maybeHideDateIndicator() {
        binding.indicatorLayout.visibility =
            if (viewModel.date[Calendar.MONTH] == Calendar.getInstance()[Calendar.MONTH] &&
                viewModel.date[Calendar.DATE] == Calendar.getInstance()[Calendar.DATE]) View.GONE else View.VISIBLE
        binding.indicatorDate.text = String.format(Locale.getDefault(), "%d", Calendar.getInstance()[Calendar.DATE])
    }

    override fun onDestroyView() {
        binding.eventsRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDatePicked(calendar: Calendar) {
        binding.eventsRecycler.visibility = View.GONE
        viewModel.date[CustomDatePicker.LEAP_YEAR, calendar[Calendar.MONTH], calendar[Calendar.DATE], 0] = 0
        binding.day.text = DateUtil.getMonthOnlyDateString(viewModel.date.time)
        binding.appBar.setExpanded(true)
        viewModel.loadOnThisDay(calendar)
        maybeHideDateIndicator()
    }

    private fun onCalendarClicked() {
        val newFragment = CustomDatePicker()
        newFragment.setSelectedDay(viewModel.date[Calendar.MONTH], viewModel.date[Calendar.DATE])
        newFragment.callback = this@OnThisDayFragment
        newFragment.show(parentFragmentManager, "datePicker")
    }

    private fun onIndicatorLayoutClicked() {
        onDatePicked(Calendar.getInstance())
    }

    private inner class RecyclerAdapter(private val events: List<OnThisDay.Event>,
                                        private val wiki: WikiSite) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == Companion.VIEW_TYPE_FOOTER) {
                val itemView = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.view_on_this_day_footer, viewGroup, false)
                FooterViewHolder(itemView)
            } else {
                val itemView = ViewEventsLayoutBinding.inflate(LayoutInflater.from(viewGroup.context),
                    viewGroup, false)
                EventsViewHolder(itemView, wiki)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is EventsViewHolder) {
                holder.setFields(events[position])
            }
        }

        override fun getItemCount(): Int {
            return events.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < events.size) Companion.VIEW_TYPE_ITEM else Companion.VIEW_TYPE_FOOTER
        }

        override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
            super.onViewAttachedToWindow(holder)
            if (holder is EventsViewHolder) {
                holder.animateRadioButton()
            }
        }
    }

    private inner class EventsViewHolder(eventBinding: ViewEventsLayoutBinding, private val wiki: WikiSite) : RecyclerView.ViewHolder(eventBinding.root) {

        private val otdEventLayout = eventBinding.otdEventLayout

        init {
            otdEventLayout.text.setTextIsSelectable(true)
        }

        fun setFields(event: OnThisDay.Event) {
            otdEventLayout.text.text = event.text
            otdEventLayout.text.visibility = if (event.text.isEmpty()) View.GONE else View.VISIBLE
            otdEventLayout.year.text = DateUtil.yearToStringWithEra(event.year)
            otdEventLayout.yearsText.text = DateUtil.getYearDifferenceString(event.year, wiki.languageCode)
            setPagesViewPager(event)
        }

        private fun setPagesViewPager(event: OnThisDay.Event) {
            event.pages()?.let {
                otdEventLayout.pagesPager.adapter = ViewPagerAdapter(it, wiki)
                otdEventLayout.pagesPager.offscreenPageLimit = 2
                TabLayoutMediator(otdEventLayout.pagesIndicator, otdEventLayout.pagesPager) { _, _ -> }.attach()
                otdEventLayout.pagesPager.visibility = View.VISIBLE
                otdEventLayout.pagesIndicator.visibility = if (it.size == 1) View.GONE else View.VISIBLE
            } ?: run {
                otdEventLayout.pagesPager.visibility = View.GONE
                otdEventLayout.pagesIndicator.visibility = View.GONE
            }
        }

        fun animateRadioButton() {
            otdEventLayout.radioImageView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.pulse))
        }
    }

    private inner class FooterViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        init {
            val backToFutureView = v.findViewById<View>(R.id.back_to_future_text_view)
            backToFutureView.setOnClickListener {
                binding.appBar.setExpanded(true)
                binding.eventsRecycler.scrollToPosition(0)
            }
        }
    }

    internal inner class ViewPagerAdapter(private val pages: List<PageSummary>, private val wiki: WikiSite) : RecyclerView.Adapter<OnThisDayPagesViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OnThisDayPagesViewHolder {
            val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_on_this_day_pages, viewGroup, false)
            return OnThisDayPagesViewHolder((viewGroup.context as AppCompatActivity), itemView, wiki)
        }

        override fun onBindViewHolder(onThisDayPagesViewHolder: OnThisDayPagesViewHolder, i: Int) {
            onThisDayPagesViewHolder.setFields(pages[i])
        }

        override fun getItemCount(): Int {
            return pages.size
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_FOOTER = 1

        fun newInstance(age: Int, wikiSite: WikiSite, year: Int, invokeSource: InvokeSource): OnThisDayFragment {
            return OnThisDayFragment().apply {
                arguments = bundleOf(OnThisDayActivity.EXTRA_AGE to age,
                    Constants.ARG_WIKISITE to wikiSite,
                    OnThisDayActivity.EXTRA_YEAR to year,
                    Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
