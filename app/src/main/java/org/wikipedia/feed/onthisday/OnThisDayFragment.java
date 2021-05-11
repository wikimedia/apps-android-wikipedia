package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.OnThisDayFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.CustomDatePicker;
import org.wikipedia.views.HeaderMarginItemDecoration;
import org.wikipedia.views.WikiErrorView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.AGE;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.WIKISITE;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.YEAR;

public class OnThisDayFragment extends Fragment implements CustomDatePicker.Callback {
    @BindView(R.id.day) TextView dayText;
    @BindView(R.id.collapsing_toolbar_layout) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.day_info_text_view) TextView dayInfoTextView;
    @BindView(R.id.events_recycler) RecyclerView eventsRecycler;
    @BindView(R.id.on_this_day_progress) ProgressBar progressBar;
    @BindView(R.id.header_frame_layout) FrameLayout headerFrameLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.on_this_day_error_view) WikiErrorView errorView;
    @BindView(R.id.indicator_date) TextView indicatorDate;
    @BindView(R.id.indicator_layout) FrameLayout indicatorLayout;
    @BindView(R.id.toolbar_day) TextView toolbarDay;
    @BindView(R.id.drop_down_toolbar) ImageView toolbarDropDown;
    @BindView(R.id.on_this_day_title_view) TextView onThisDayTitleView;

    @Nullable private OnThisDay onThisDay;
    private Calendar date;
    private Unbinder unbinder;
    @Nullable private OnThisDayFunnel funnel;
    private WikiSite wiki;
    private int yearOnCardView;
    private int positionToScrollTo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @NonNull
    public static OnThisDayFragment newInstance(int age, WikiSite wikiSite) {
        OnThisDayFragment instance = new OnThisDayFragment();
        Bundle args = new Bundle();
        args.putInt(AGE, age);
        args.putParcelable(WIKISITE, wikiSite);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_on_this_day, container, false);
        unbinder = ButterKnife.bind(this, view);
        int age = requireActivity().getIntent().getIntExtra(AGE, 0);
        wiki = requireActivity().getIntent().getParcelableExtra(WIKISITE);
        date = DateUtil.getDefaultDateFor(age);
        yearOnCardView = requireActivity().getIntent().getIntExtra(YEAR, -1);
        setUpToolbar();
        eventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        final int topDecorationDp = 24;
        eventsRecycler.addItemDecoration(new HeaderMarginItemDecoration(topDecorationDp, 0));
        setUpRecycler(eventsRecycler);
        errorView.setBackClickListener(v -> requireActivity().finish());

        setUpTransitionAnimation(savedInstanceState, age);

        progressBar.setVisibility(GONE);
        eventsRecycler.setVisibility(GONE);
        errorView.setVisibility(GONE);
        return view;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setUpTransitionAnimation(@Nullable Bundle savedInstanceState, int age) {
        final int animDelay = (requireActivity().getWindow().getSharedElementEnterTransition() != null
                && savedInstanceState == null) ? 500 : 0;
        onThisDayTitleView.postDelayed(() -> {
            if (!isAdded() || onThisDayTitleView == null) {
                return;
            }
            updateContents(age);
        }, animDelay);
    }

    private void updateContents(int age) {
        Calendar today = DateUtil.getDefaultDateFor(age);
        requestEvents(today.get(Calendar.MONTH), today.get(Calendar.DATE));
        funnel = new OnThisDayFunnel(WikipediaApp.getInstance(), wiki,
                (InvokeSource) requireActivity().getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void requestEvents(int month, int date) {
        progressBar.setVisibility(VISIBLE);
        eventsRecycler.setVisibility(GONE);
        errorView.setVisibility(GONE);

        disposables.add(ServiceFactory.getRest(wiki).getOnThisDay(month + 1, date)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    progressBar.setVisibility(GONE);
                    eventsRecycler.postDelayed(() -> {
                        if (positionToScrollTo != -1 && yearOnCardView != -1) {
                            eventsRecycler.scrollToPosition(positionToScrollTo);
                        }
                    }, 500);
                })
                .subscribe(response -> {
                    onThisDay = response;
                    eventsRecycler.setVisibility(VISIBLE);
                    eventsRecycler.setAdapter(new RecyclerAdapter(onThisDay.events(), wiki));
                    List<OnThisDay.Event> events = onThisDay.events();
                    for (int i = 0; i < events.size(); i++) {
                        if (yearOnCardView == events.get(i).year()) {
                            positionToScrollTo = i;
                            break;
                        }
                    }
                    int beginningYear = events.get(events.size() - 1).year();
                    dayInfoTextView.setText(String.format(getString(R.string.events_count_text), Integer.toString(events.size()),
                            DateUtil.yearToStringWithEra(beginningYear), events.get(0).year()));
                }, throwable -> {
                    L.e(throwable);
                    errorView.setError(throwable);
                    errorView.setVisibility(VISIBLE);
                    eventsRecycler.setVisibility(GONE);
                }));
    }

    private void setUpToolbar() {
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");
        collapsingToolbarLayout.setCollapsedTitleTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_primary_color));
        dayText.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        maybeHideDateIndicator();
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            headerFrameLayout.setAlpha(1.0f - Math.abs(verticalOffset / (float) appBarLayout.getTotalScrollRange()));
            if (verticalOffset > -appBarLayout.getTotalScrollRange()) {
                toolbarDropDown.setVisibility(GONE);
            } else if (verticalOffset <= -appBarLayout.getTotalScrollRange()) {
                toolbarDropDown.setVisibility(VISIBLE);
            }
            final String newText = verticalOffset <= -appBarLayout.getTotalScrollRange()
                    ? DateUtil.getMonthOnlyDateString(date.getTime()) : "";
            if (!newText.equals(toolbarDay.getText().toString())) {
                appBarLayout.post(() -> toolbarDay.setText(newText));
            }
        });
    }

    private void maybeHideDateIndicator() {
        indicatorLayout.setVisibility((date.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) && date.get(Calendar.DATE) == Calendar.getInstance().get(Calendar.DATE)) ? GONE : VISIBLE);
        indicatorDate.setText(String.format(Locale.getDefault(), "%d", Calendar.getInstance().get(Calendar.DATE)));
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        eventsRecycler.setAdapter(null);
        if (funnel != null && eventsRecycler.getAdapter() != null) {
            funnel.done(eventsRecycler.getAdapter().getItemCount());
            funnel = null;
        }
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private void setUpRecycler(RecyclerView recycler) {
        recycler.setNestedScrollingEnabled(true);
        recycler.setClipToPadding(false);
    }

    @Override
    public void onDatePicked(int month, int day) {
        eventsRecycler.setVisibility(GONE);
        date.set(CustomDatePicker.LEAP_YEAR, month, day, 0, 0);
        dayText.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        appBarLayout.setExpanded(true);
        requestEvents(month, day);
        maybeHideDateIndicator();
    }

    @OnClick({R.id.day_container, R.id.toolbar_day_container})
    public void onCalendarClicked() {
        CustomDatePicker newFragment = new CustomDatePicker();
        newFragment.setSelectedDay(date.get(Calendar.MONTH), date.get(Calendar.DATE));
        newFragment.setCallback(OnThisDayFragment.this);
        newFragment.show(requireFragmentManager(), "date picker");
    }

    @OnClick(R.id.indicator_layout)
    public void onIndicatorLayoutClicked() {
        onDatePicked(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DATE));
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_FOOTER = 1;
        private List<OnThisDay.Event> events;
        private WikiSite wiki;

        RecyclerAdapter(List<OnThisDay.Event> events, WikiSite wiki) {
            this.wiki = wiki;
            this.events = events;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            if (viewType == VIEW_TYPE_FOOTER) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.view_on_this_day_footer, viewGroup, false);
                return new FooterViewHolder(itemView);
            } else {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.view_events_layout, viewGroup, false);
                return new EventsViewHolder(itemView, wiki);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof EventsViewHolder) {
                ((EventsViewHolder) holder).setFields(events.get(position));
                if (funnel != null) {
                    funnel.scrolledToPosition(position);
                }
            }
        }

        @Override
        public int getItemCount() {
            return events.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position < events.size() ? VIEW_TYPE_ITEM : VIEW_TYPE_FOOTER;
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof EventsViewHolder) {
                ((EventsViewHolder) holder).animateRadioButton();
            }
        }
    }

    private class EventsViewHolder extends RecyclerView.ViewHolder {
        private TextView descTextView;
        private TextView yearTextView;
        private TextView yearsInfoTextView;
        private ViewPager2 pagesViewPager;
        private TabLayout pagesIndicator;
        private ImageView radioButtonImageView;
        private WikiSite wiki;

        EventsViewHolder(View v, WikiSite wiki) {
            super(v);
            descTextView = v.findViewById(R.id.text);
            descTextView.setTextIsSelectable(true);
            yearTextView = v.findViewById(R.id.year);
            yearsInfoTextView = v.findViewById(R.id.years_text);
            pagesViewPager = v.findViewById(R.id.pages_pager);
            pagesIndicator = v.findViewById(R.id.pages_indicator);
            radioButtonImageView = v.findViewById(R.id.radio_image_view);
            this.wiki = wiki;
        }

        public void setFields(@NonNull final OnThisDay.Event event) {
            descTextView.setText(event.text());
            descTextView.setVisibility(TextUtils.isEmpty(event.text()) ? GONE : VISIBLE);
            yearTextView.setText(DateUtil.yearToStringWithEra(event.year()));
            yearsInfoTextView.setText(DateUtil.getYearDifferenceString(event.year()));
            setPagesViewPager(event);
        }

        private void setPagesViewPager(OnThisDay.Event event) {
            if (event.pages() != null) {
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager(), event.pages(), wiki);
                pagesViewPager.setAdapter(viewPagerAdapter);
                pagesViewPager.setOffscreenPageLimit(2);
                 new TabLayoutMediator(pagesIndicator, pagesViewPager, (tab, position) -> { }).attach();
                pagesViewPager.setVisibility(VISIBLE);
                pagesIndicator.setVisibility(event.pages().size() == 1 ? GONE : VISIBLE);
            } else {
                pagesViewPager.setVisibility(GONE);
                pagesIndicator.setVisibility(GONE);
            }
        }

        public void animateRadioButton() {
            Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
            radioButtonImageView.startAnimation(pulse);
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View v) {
            super(v);
            View backToFutureView = v.findViewById(R.id.back_to_future_text_view);
            backToFutureView.setOnClickListener(v1 -> {
                appBarLayout.setExpanded(true);
                eventsRecycler.scrollToPosition(0);
            });
        }
    }
     class ViewPagerAdapter extends RecyclerView.Adapter<OnThisDayPagesViewHolder> {
        private List<PageSummary> pages;
        private WikiSite wiki;
        private FragmentManager fragmentManager;

         ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull List<PageSummary> pages, @NonNull WikiSite wiki) {
            this.pages = pages;
            this.wiki = wiki;
            this.fragmentManager = fragmentManager;
        }

        @NonNull @Override
        public OnThisDayPagesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.item_on_this_day_pages, viewGroup, false);
            return new OnThisDayPagesViewHolder((Activity) viewGroup.getContext(), fragmentManager, itemView, wiki);
        }

        @Override
        public void onBindViewHolder(@NonNull OnThisDayPagesViewHolder onThisDayPagesViewHolder, int i) {
            onThisDayPagesViewHolder
                    .setFields(pages.get(i));
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }
}
