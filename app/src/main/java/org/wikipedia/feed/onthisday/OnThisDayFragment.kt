package org.wikipedia.feed.onthisday

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.OnThisDayFunnel
import org.wikipedia.databinding.FragmentOnThisDayBinding
import org.wikipedia.databinding.ViewEventsLayoutBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CustomDatePicker
import org.wikipedia.views.HeaderMarginItemDecoration
import java.util.*
import kotlin.math.abs

class OnThisDayFragment : Fragment(), CustomDatePicker.Callback {

    private var _binding: FragmentOnThisDayBinding? = null
    private val binding get() = _binding!!

    private lateinit var date: Calendar
    private lateinit var wiki: WikiSite
    private lateinit var invokeSource: InvokeSource
    private var funnel: OnThisDayFunnel? = null
    private var yearOnCardView = 0
    private var positionToScrollTo = 0
    private val disposables = CompositeDisposable()
    private val appCompatActivity get() = requireActivity() as AppCompatActivity
    private var onThisDay: OnThisDay? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        _binding = FragmentOnThisDayBinding.inflate(inflater, container, false)

        val topDecorationDp = 24
        val age = requireArguments().getInt(OnThisDayActivity.EXTRA_AGE, 0)
        wiki = requireArguments().getParcelable(OnThisDayActivity.EXTRA_WIKISITE)!!
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        date = DateUtil.getDefaultDateFor(age)
        yearOnCardView = requireArguments().getInt(OnThisDayActivity.EXTRA_YEAR, -1)
        setUpToolbar()
        binding.eventsRecycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.eventsRecycler.addItemDecoration(HeaderMarginItemDecoration(topDecorationDp, 0))
        setUpRecycler(binding.eventsRecycler)
        setUpTransitionAnimation(savedInstanceState, age)
        binding.progressBar.visibility = View.GONE
        binding.eventsRecycler.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.dayContainer.setOnClickListener { onCalendarClicked() }
        binding.toolbarDayContainer.setOnClickListener { onCalendarClicked() }
        binding.indicatorLayout.setOnClickListener { onIndicatorLayoutClicked() }
        return binding.root
    }

    private fun setUpTransitionAnimation(savedInstanceState: Bundle?, age: Int) {
        val animDelay = if (requireActivity().window.sharedElementEnterTransition != null &&
            savedInstanceState == null) 500L else 0L
        binding.onThisDayTitleView.postDelayed({
            if (!isAdded) {
                return@postDelayed
            }
            updateContents(age)
        }, animDelay)
    }

    private fun updateContents(age: Int) {
        val today = DateUtil.getDefaultDateFor(age)
        requestEvents(today[Calendar.MONTH], today[Calendar.DATE])
        funnel = OnThisDayFunnel(WikipediaApp.getInstance(), wiki, invokeSource)
    }

    private fun requestEvents(month: Int, date: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.eventsRecycler.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        disposables.add(ServiceFactory.getRest(wiki).getOnThisDay(month + 1, date)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                if (!isAdded) {
                    return@doAfterTerminate
                }
                binding.progressBar.visibility = View.GONE
                binding.eventsRecycler.postDelayed({
                    if (positionToScrollTo != -1 && yearOnCardView != -1) {
                        binding.eventsRecycler.scrollToPosition(positionToScrollTo)
                    }
                }, 500)
            }
            .subscribe({ response ->
                onThisDay = response
                onThisDay?.let { onThisDayResponse ->
                    binding.eventsRecycler.visibility = View.VISIBLE
                    binding.eventsRecycler.adapter = RecyclerAdapter(onThisDayResponse.allEvents(), wiki)
                    val events = onThisDayResponse.allEvents()
                    positionToScrollTo = events.indices.find { yearOnCardView == events[it].year } ?: 0
                    val beginningYear = events.last().year
                    binding.dayInfo.text = getString(R.string.events_count_text, events.size.toString(),
                        DateUtil.yearToStringWithEra(beginningYear), events[0].year)
                }
            }) { throwable ->
                L.e(throwable)
                binding.errorView.setError(throwable)
                binding.errorView.visibility = View.VISIBLE
                binding.eventsRecycler.visibility = View.GONE
            })
    }

    private fun setUpToolbar() {
        appCompatActivity.setSupportActionBar(binding.toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        appCompatActivity.supportActionBar?.title = ""
        binding.collapsingToolbarLayout.setCollapsedTitleTextColor(
            ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_primary_color)
        )
        binding.day.text = DateUtil.getMonthOnlyDateString(date.time)
        maybeHideDateIndicator()
        binding.appBar.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.headerFrameLayout.alpha = 1.0f - abs(verticalOffset / appBarLayout.totalScrollRange.toFloat())
            if (verticalOffset > -appBarLayout.totalScrollRange) {
                binding.dropDownToolbar.visibility = View.GONE
            } else if (verticalOffset <= -appBarLayout.totalScrollRange) {
                binding.dropDownToolbar.visibility = View.VISIBLE
            }
            val newText = if (verticalOffset <= -appBarLayout.totalScrollRange) DateUtil.getMonthOnlyDateString(date.time) else ""
            if (newText != binding.toolbarDay.text.toString()) {
                appBarLayout.post { binding.toolbarDay.text = newText }
            }
        })
    }

    private fun maybeHideDateIndicator() {
        binding.indicatorLayout.visibility =
            if (date[Calendar.MONTH] == Calendar.getInstance()[Calendar.MONTH] &&
                date[Calendar.DATE] == Calendar.getInstance()[Calendar.DATE]) View.GONE else View.VISIBLE
        binding.indicatorDate.text = String.format(Locale.getDefault(), "%d", Calendar.getInstance()[Calendar.DATE])
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.eventsRecycler.adapter = null
        if (binding.eventsRecycler.adapter != null) {
            funnel?.done(binding.eventsRecycler.adapter!!.itemCount)
            funnel = null
        }
        _binding = null
        super.onDestroyView()
    }

    private fun setUpRecycler(recycler: RecyclerView) {
        recycler.isNestedScrollingEnabled = true
        recycler.clipToPadding = false
    }

    override fun onDatePicked(month: Int, day: Int) {
        binding.eventsRecycler.visibility = View.GONE
        date[CustomDatePicker.LEAP_YEAR, month, day, 0] = 0
        binding.day.text = DateUtil.getMonthOnlyDateString(date.time)
        binding.appBar.setExpanded(true)
        requestEvents(month, day)
        maybeHideDateIndicator()
    }

    private fun onCalendarClicked() {
        val newFragment = CustomDatePicker()
        newFragment.setSelectedDay(date[Calendar.MONTH], date[Calendar.DATE])
        newFragment.callback = this@OnThisDayFragment
        newFragment.show(parentFragmentManager, "date picker")
    }

    private fun onIndicatorLayoutClicked() {
        onDatePicked(Calendar.getInstance()[Calendar.MONTH], Calendar.getInstance()[Calendar.DATE])
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
                funnel?.scrolledToPosition(position)
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
                val viewPagerAdapter = ViewPagerAdapter(childFragmentManager, it, wiki)
                otdEventLayout.pagesPager.adapter = viewPagerAdapter
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
            val pulse = AnimationUtils.loadAnimation(context, R.anim.pulse)
            otdEventLayout.radioImageView.startAnimation(pulse)
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

    internal inner class ViewPagerAdapter(private val fragmentManager: FragmentManager,
                                          private val pages: List<PageSummary>, private val wiki: WikiSite) : RecyclerView.Adapter<OnThisDayPagesViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OnThisDayPagesViewHolder {
            val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_on_this_day_pages, viewGroup, false)
            return OnThisDayPagesViewHolder((viewGroup.context as Activity), fragmentManager, itemView, wiki)
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
                    OnThisDayActivity.EXTRA_WIKISITE to wikiSite,
                    OnThisDayActivity.EXTRA_YEAR to year,
                    Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
