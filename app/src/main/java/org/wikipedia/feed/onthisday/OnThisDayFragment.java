package org.wikipedia.feed.onthisday;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.OnThisDayFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.CustomDatePicker;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.HeaderMarginItemDecoration;
import org.wikipedia.views.MarginItemDecoration;
import org.wikipedia.views.WikiErrorView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_ACTIVITY;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.AGE;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.WIKISITE;

public class OnThisDayFragment extends Fragment implements CustomDatePicker.Callback, OnThisDayActionsDialog.Callback{
    @BindView(R.id.day) TextView dayText;
    @BindView(R.id.collapsing_toolbar_layout) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.day_info_text_view) TextView dayInfoTextView;
    @BindView(R.id.events_recycler) RecyclerView eventsRecycler;
    @BindView(R.id.on_this_day_progress) ProgressBar progressBar;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.on_this_day_error_view) WikiErrorView errorView;
    @BindView(R.id.indicator_date) TextView indicatorDate;
    @BindView(R.id.indicator_layout) FrameLayout indicatorLayout;
    @BindView(R.id.toolbar_day) TextView toolbarDay;
    @BindView(R.id.drop_down_toolbar) ImageView toolbarDropDown;

    @Nullable private OnThisDay onThisDay;
    private Calendar date;
    private Unbinder unbinder;
    @Nullable private OnThisDayFunnel funnel;
    public static final int PADDING1 = 21, PADDING2 = 38, PADDING3 = 21;
    public static final float HALF_ALPHA = 0.5f;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private WikiSite wiki;
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
        setUpToolbar();
        eventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        final int topDecorationDp = 24;
        eventsRecycler.addItemDecoration(new HeaderMarginItemDecoration(topDecorationDp, 0));
        setUpRecycler(eventsRecycler);

        errorView.setBackClickListener(v -> requireActivity().finish());

        if (requireActivity().getWindow().getSharedElementEnterTransition() != null
                && savedInstanceState == null) {
            final int animDelay = 500;
            dayText.postDelayed(() -> {
                if (!isAdded() || dayText == null) {
                    return;
                }
                updateContents(age);
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), dayText.getCurrentTextColor(),
                        ResourceUtil.getThemedColor(requireContext(), R.attr.toolbar_title_color));
                colorAnimation.addUpdateListener(animator -> {
                    if (dayText != null) {
                        dayText.setTextColor((Integer) animator.getAnimatedValue());
                    }
                });
                colorAnimation.start();
            }, animDelay);
        } else {
            dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.toolbar_title_color));
            updateContents(age);
        }

        eventsRecycler.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        return view;
    }

    public void onBackPressed() {
        dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color));
    }

    private void updateContents(int age) {
        Calendar today = DateUtil.getDefaultDateFor(age);
        requestEvents(today.get(Calendar.MONTH), today.get(Calendar.DATE));
        funnel = new OnThisDayFunnel(WikipediaApp.getInstance(), wiki,
                (InvokeSource) requireActivity().getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE));
    }

    private void requestEvents(int month, int date) {
        progressBar.setVisibility(View.VISIBLE);
        eventsRecycler.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);

        disposables.add(ServiceFactory.getRest(wiki).getOnThisDay(month + 1, date)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> progressBar.setVisibility(View.GONE))
                .subscribe(response -> {
                    onThisDay = response;
                    eventsRecycler.setVisibility(View.VISIBLE);
                    eventsRecycler.setAdapter(new RecyclerAdapter(onThisDay.events(), wiki));
                    List<OnThisDay.Event> events = onThisDay.events();
                    int beginningYear = events.get(events.size() - 1).year();
                    dayInfoTextView.setText(String.format(getString(R.string.events_count_text), Integer.toString(events.size()),
                            DateUtil.yearToStringWithEra(beginningYear), events.get(0).year()));
                }, throwable -> {
                    L.e(throwable);
                    errorView.setError(throwable);
                    errorView.setVisibility(View.VISIBLE);
                    eventsRecycler.setVisibility(View.GONE);
                }));
    }

    private void setUpToolbar() {
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");
        collapsingToolbarLayout.setCollapsedTitleTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.toolbar_title_color));
        dayText.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        indicatorLayout.setAlpha((date.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) && date.get(Calendar.DATE) == Calendar.getInstance().get(Calendar.DATE)) ? HALF_ALPHA : 1.0f);
        indicatorDate.setText(String.format(Locale.getDefault(), "%d", Calendar.getInstance().get(Calendar.DATE)));
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (verticalOffset > -appBarLayout.getTotalScrollRange()) {
                toolbarDropDown.setVisibility(View.GONE);
            } else if (verticalOffset <= -appBarLayout.getTotalScrollRange()) {
                toolbarDropDown.setVisibility(View.VISIBLE);
            }
            final String newText = verticalOffset <= -appBarLayout.getTotalScrollRange()
                    ? DateUtil.getMonthOnlyDateString(date.getTime()) : "";
            if (!newText.equals(toolbarDay.getText().toString())) {
                appBarLayout.post(() -> toolbarDay.setText(newText));
            }
        });
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
        recycler.addItemDecoration(new MarginItemDecoration(requireContext(),
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical));
        recycler.addOnItemTouchListener(new DontInterceptTouchListener());
        recycler.setNestedScrollingEnabled(true);
        recycler.setClipToPadding(false);
    }

    @Override
    public void onDatePicked(int month, int day) {
        eventsRecycler.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        if (Calendar.getInstance().get(Calendar.MONTH) != month || Calendar.getInstance().get(Calendar.DATE) != day) {
            indicatorLayout.setAlpha(1.0f);
            indicatorLayout.setClickable(true);
        } else {
            indicatorLayout.setAlpha(HALF_ALPHA);
            indicatorLayout.setClickable(false);
        }
        date.set(CustomDatePicker.LEAP_YEAR, month, day, 0, 0);
        dayText.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        appBarLayout.setExpanded(true);
        requestEvents(month, day);
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
        indicatorLayout.setAlpha(HALF_ALPHA);
        indicatorLayout.setClickable(false);
    }

    @Override
    public void onAddPageToList(@NonNull HistoryEntry entry) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                AddToReadingListDialog.newInstance(entry.getTitle(), ON_THIS_DAY_ACTIVITY));
    }

    @Override
    public void onSharePage(@NonNull HistoryEntry entry) {
        ShareUtil.shareText(getActivity(), entry.getTitle());
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
                ((EventsViewHolder) holder).setFields(events.get(position),
                        position > 0 ? events.get(position - 1) : null);
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
    }

    private class EventsViewHolder extends RecyclerView.ViewHolder {
        private TextView descTextView;
        private TextView yearTextView;
        private TextView yearsInfoTextView;
        private RecyclerView pagesRecycler;
        private View yearContainer;
        private View yearSpace;
        private WikiSite wiki;

        EventsViewHolder(View v, WikiSite wiki) {
            super(v);
            descTextView = v.findViewById(R.id.text);
            descTextView.setTextIsSelectable(true);
            yearTextView = v.findViewById(R.id.year);
            yearsInfoTextView = v.findViewById(R.id.years_text);
            pagesRecycler = v.findViewById(R.id.pages_recycler);
            yearContainer = v.findViewById(R.id.years_text_container);
            yearSpace = v.findViewById(R.id.years_text_space);
            this.wiki = wiki;
            setRecycler();
        }

        private void setRecycler() {
            if (pagesRecycler != null) {
                pagesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                setUpRecycler(pagesRecycler);
            }
        }

        public void setFields(@NonNull final OnThisDay.Event event, @Nullable OnThisDay.Event prevEvent) {
            setPagesRecycler(event);
            setPads();
            descTextView.setText(event.text());
            yearTextView.setText(DateUtil.yearToStringWithEra(event.year()));
            yearsInfoTextView.setText(DateUtil.getYearDifferenceString(event.year()));
            if (prevEvent != null && prevEvent.year() == event.year()) {
                yearContainer.setVisibility(View.GONE);
                yearSpace.setVisibility(View.GONE);
            } else {
                yearContainer.setVisibility(View.VISIBLE);
                yearSpace.setVisibility(prevEvent == null ? View.GONE : View.VISIBLE);
            }
        }

        private void setPads() {
            int pad1 = (int) DimenUtil.dpToPx(PADDING1);
            int pad2 = (int) DimenUtil.dpToPx(PADDING2);
            int pad3 = (int) DimenUtil.dpToPx(PADDING3);

            descTextView.setPaddingRelative(pad1, 0, 0, 0);
            pagesRecycler.setPaddingRelative(pad2, 0, 0, 0);
            yearTextView.setPaddingRelative(pad3, 0, 0, 0);
        }

        private void setPagesRecycler(OnThisDay.Event event) {
            if (event.pages() != null) {
                OnThisDayCardView.RecyclerAdapter recyclerAdapter = new OnThisDayCardView.RecyclerAdapter(event.pages(), wiki, false);
                recyclerAdapter.setCallback(new ItemCallback());
                pagesRecycler.setAdapter(recyclerAdapter);
            } else {
                pagesRecycler.setVisibility(View.GONE);
            }
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View v) {
            super(v);
            View backToTopView = v.findViewById(R.id.back_to_top_view);
            backToTopView.setOnClickListener(v1 -> {
                appBarLayout.setExpanded(true);
                eventsRecycler.scrollToPosition(0);
            });
        }
    }

    class ItemCallback implements OnThisDayPagesViewHolder.ItemCallBack {
        @Override
        public void onActionLongClick(@NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    OnThisDayActionsDialog.newInstance(entry));
        }
    }
}
