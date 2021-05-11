package org.wikipedia.feed.onthisday

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.tabs.TabLayout
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
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.DateUtil.getDefaultDateFor
import org.wikipedia.util.DateUtil.getMonthOnlyDateString
import org.wikipedia.util.DateUtil.getYearDifferenceString
import org.wikipedia.util.DateUtil.yearToStringWithEra
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.log.L.e
import org.wikipedia.views.CustomDatePicker
import org.wikipedia.views.HeaderMarginItemDecoration
import java.util.*
import kotlin.math.abs

class OnThisDayFragment : Fragment(), CustomDatePicker.Callback {

    private var _binding: FragmentOnThisDayBinding? = null
    private val binding get() = _binding!!
    private val disposables = CompositeDisposable()
    private val appCompatActivity get() = activity as AppCompatActivity?
    private var onThisDay: OnThisDay? = null
    private var date: Calendar? = null
    private var funnel: OnThisDayFunnel? = null
    private var wiki: WikiSite? = null
    private var yearOnCardView = 0
    private var positionToScrollTo = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val topDecorationDp = 24
        val age = requireActivity().intent.getIntExtra(OnThisDayActivity.AGE, 0)

        _binding = FragmentOnThisDayBinding.inflate(inflater, container, false)
        wiki = requireActivity().intent.getParcelableExtra(OnThisDayActivity.WIKISITE)
        date = getDefaultDateFor(age)
        yearOnCardView = requireActivity().intent.getIntExtra(OnThisDayActivity.YEAR, -1)
        setUpToolbar()
        binding.eventsRecycler.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.eventsRecycler.addItemDecoration(HeaderMarginItemDecoration(topDecorationDp, 0))
        setUpRecycler(binding.eventsRecycler)
        setUpTransitionAnimation(savedInstanceState, age)
        binding.progressBar.visibility = View.GONE
        binding.eventsRecycler.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.errorView.backClickListener =
            View.OnClickListener { requireActivity().finish() }
        binding.dayContainer.setOnClickListener { onCalendarClicked() }
        binding.toolbarDayContainer.setOnClickListener { onCalendarClicked() }
        binding.indicatorLayout.setOnClickListener { onIndicatorLayoutClicked() }
        return binding.root
    }

    private fun setUpTransitionAnimation(savedInstanceState: Bundle?, age: Int) {
        val animDelay =
            if (requireActivity().window.sharedElementEnterTransition != null && savedInstanceState == null
            ) 500 else 0
        binding.onThisDayTitleView.postDelayed({
            if (!isAdded) {
                return@postDelayed
            }
            updateContents(age)
        }, animDelay.toLong())
    }

    private fun updateContents(age: Int) {
        val today = getDefaultDateFor(age)
        requestEvents(today[Calendar.MONTH], today[Calendar.DATE])
        funnel = OnThisDayFunnel(
            WikipediaApp.getInstance(), wiki,
            (requireActivity().intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource?)!!
        )
    }

    private fun requestEvents(month: Int, date: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.eventsRecycler.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        disposables.add(ServiceFactory.getRest(wiki!!).getOnThisDay(month + 1, date)
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
                binding.eventsRecycler.visibility = View.VISIBLE
                binding.eventsRecycler.adapter = RecyclerAdapter(
                    onThisDay!!.events(), wiki!!
                )
                val events = onThisDay!!.events()
                for (i in events.indices) {
                    if (yearOnCardView == events[i].year()) {
                        positionToScrollTo = i
                        break
                    }
                }
                val beginningYear = events[events.size - 1].year()
                binding.dayInfo.text = String.format(
                    getString(R.string.events_count_text), events.size.toString(),
                    yearToStringWithEra(beginningYear), events[0].year()
                )
            }) { throwable ->
                e(throwable)
                binding.errorView.setError(throwable)
                binding.errorView.visibility = View.VISIBLE
                binding.eventsRecycler.visibility = View.GONE
            })
    }

    private fun setUpToolbar() {
        appCompatActivity!!.setSupportActionBar(binding.toolbar)
        appCompatActivity!!.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appCompatActivity!!.supportActionBar!!.title = ""
        binding.collapsingToolbarLayout.setCollapsedTitleTextColor(
            getThemedColor(
                requireContext(),
                R.attr.material_theme_primary_color
            )
        )
        binding.day.text = getMonthOnlyDateString(date!!.time)
        maybeHideDateIndicator()
        binding.appBar.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.headerFrameLayout.alpha = 1.0f - abs(
                verticalOffset / appBarLayout.totalScrollRange
                    .toFloat()
            )
            if (verticalOffset > -appBarLayout.totalScrollRange) {
                binding.dropDownToolbar.visibility = View.GONE
            } else if (verticalOffset <= -appBarLayout.totalScrollRange) {
                binding.dropDownToolbar.visibility = View.VISIBLE
            }
            val newText =
                if (verticalOffset <= -appBarLayout.totalScrollRange) getMonthOnlyDateString(
                    date!!.time
                ) else ""
            if (newText != binding.toolbarDay.text.toString()) {
                appBarLayout.post { binding.toolbarDay.text = newText }
            }
        })
    }

    private fun maybeHideDateIndicator() {
        binding.indicatorLayout.visibility =
            if (date!![Calendar.MONTH] == Calendar.getInstance()[Calendar.MONTH] && date!![Calendar.DATE] == Calendar.getInstance()[Calendar.DATE]
            ) View.GONE else View.VISIBLE
        binding.indicatorDate.text = String.format(
            Locale.getDefault(),
            "%d",
            Calendar.getInstance()[Calendar.DATE]
        )
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.eventsRecycler.adapter = null
        if (funnel != null && binding.eventsRecycler.adapter != null) {
            funnel!!.done(binding.eventsRecycler.adapter!!.itemCount)
            funnel = null
        }
        _binding = null
        super.onDestroyView()
    }

    private fun setUpRecycler(recycler: RecyclerView?) {
        recycler!!.isNestedScrollingEnabled = true
        recycler.clipToPadding = false
    }

    override fun onDatePicked(month: Int, day: Int) {
        binding.eventsRecycler.visibility = View.GONE
        date!![CustomDatePicker.LEAP_YEAR, month, day, 0] = 0
        binding.day.text = getMonthOnlyDateString(date!!.time)
        binding.appBar.setExpanded(true)
        requestEvents(month, day)
        maybeHideDateIndicator()
    }

    fun onCalendarClicked() {
        val newFragment = CustomDatePicker()
        newFragment.setSelectedDay(date!![Calendar.MONTH], date!![Calendar.DATE])
        newFragment.callback = this@OnThisDayFragment
        newFragment.show(requireFragmentManager(), "date picker")
    }

    fun onIndicatorLayoutClicked() {
        onDatePicked(Calendar.getInstance()[Calendar.MONTH], Calendar.getInstance()[Calendar.DATE])
    }

    private inner class RecyclerAdapter(
        private val events: List<OnThisDay.Event>,
        private val wiki: WikiSite
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(
            viewGroup: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return if (viewType == Companion.VIEW_TYPE_FOOTER) {
                val itemView = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.view_on_this_day_footer, viewGroup, false)
                FooterViewHolder(itemView)
            } else {
                val itemView = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.view_events_layout, viewGroup, false)
                EventsViewHolder(itemView, wiki)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is EventsViewHolder) {
                holder.setFields(events[position])
                if (funnel != null) {
                    funnel!!.scrolledToPosition(position)
                }
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

    private inner class EventsViewHolder(v: View, wiki: WikiSite) :
        RecyclerView.ViewHolder(v) {
        private val descTextView: TextView = v.findViewById(R.id.text)
        private val yearTextView: TextView
        private val yearsInfoTextView: TextView
        private val pagesViewPager: ViewPager2
        private val pagesIndicator: TabLayout
        private val radioButtonImageView: ImageView
        private val wiki: WikiSite

        init {
            descTextView.setTextIsSelectable(true)
            yearTextView = v.findViewById(R.id.year)
            yearsInfoTextView = v.findViewById(R.id.years_text)
            pagesViewPager = v.findViewById(R.id.pages_pager)
            pagesIndicator = v.findViewById(R.id.pages_indicator)
            radioButtonImageView = v.findViewById(R.id.radio_image_view)
            this.wiki = wiki
        }

        fun setFields(event: OnThisDay.Event) {
            descTextView.text = event.text()
            descTextView.visibility =
                if (TextUtils.isEmpty(event.text())) View.GONE else View.VISIBLE
            yearTextView.text = yearToStringWithEra(event.year())
            yearsInfoTextView.text = getYearDifferenceString(event.year())
            setPagesViewPager(event)
        }

        private fun setPagesViewPager(event: OnThisDay.Event) {
            if (event.pages() != null) {
                val viewPagerAdapter = ViewPagerAdapter(
                    childFragmentManager, event.pages()!!, wiki
                )
                pagesViewPager.adapter = viewPagerAdapter
                pagesViewPager.offscreenPageLimit = 2
                TabLayoutMediator(
                    pagesIndicator,
                    pagesViewPager
                ) { _, _ -> }.attach()
                pagesViewPager.visibility = View.VISIBLE
                pagesIndicator.visibility =
                    if (event.pages()!!.size == 1) View.GONE else View.VISIBLE
            } else {
                pagesViewPager.visibility = View.GONE
                pagesIndicator.visibility = View.GONE
            }
        }

        fun animateRadioButton() {
            val pulse = AnimationUtils.loadAnimation(context, R.anim.pulse)
            radioButtonImageView.startAnimation(pulse)
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

    internal inner class ViewPagerAdapter(
        private val fragmentManager: FragmentManager,
        private val pages: List<PageSummary>,
        private val wiki: WikiSite
    ) : RecyclerView.Adapter<OnThisDayPagesViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OnThisDayPagesViewHolder {
            val itemView = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_on_this_day_pages, viewGroup, false)
            return OnThisDayPagesViewHolder(
                (viewGroup.context as Activity),
                fragmentManager,
                itemView,
                wiki
            )
        }

        override fun onBindViewHolder(onThisDayPagesViewHolder: OnThisDayPagesViewHolder, i: Int) {
            onThisDayPagesViewHolder
                .setFields(pages[i])
        }

        override fun getItemCount(): Int {
            return pages.size
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_FOOTER = 1

        fun newInstance(age: Int, wikiSite: WikiSite?): OnThisDayFragment {
            val instance = OnThisDayFragment()
            val args = Bundle()
            args.putInt(OnThisDayActivity.AGE, age)
            args.putParcelable(OnThisDayActivity.WIKISITE, wikiSite)
            instance.arguments = args
            return instance
        }
    }
}
