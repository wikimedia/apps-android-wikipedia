package org.wikipedia.feed.onthisday;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.OnThisDayFunnel;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DatePickerFragment;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.GONE;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.AGE;

public class OnThisDayFragment extends Fragment implements DatePickerFragment.Callback {
    @BindView(R.id.day) TextView dayText;
    @BindView(R.id.day_text_view) TextView dayTextView;
    @BindView(R.id.day_info_text_view) TextView dayInfoTextView;
    @BindView(R.id.events_recycler) RecyclerView eventsRecycler;
    @BindView(R.id.progress) ProgressBar progressBar;
    @BindView(R.id.back_to_top_view) RelativeLayout backToTopView;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.calendar) ImageView calendar;
    @BindView(R.id.nested) NestedScrollView nestedScrollView;
    @BindView(R.id.linear_layout) LinearLayout linearLayout;
    @Nullable private OnThisDay onThisDay;
    @Nullable private WikiSite wiki;
    private Calendar date;
    private Unbinder unbinder;
    @Nullable private OnThisDayFunnel funnel;

    @NonNull
    public static OnThisDayFragment newInstance(int age) {
        OnThisDayFragment instance = new OnThisDayFragment();
        Bundle args = new Bundle();
        args.putInt(AGE, age);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_on_this_day, container, false);
        unbinder = ButterKnife.bind(this, view);
        wiki = WikipediaApp.getInstance().getWikiSite();
        int age = getActivity().getIntent().getIntExtra(AGE, 0);
        OnThisDayFragment.this.date = DateUtil.getUtcRequestDateFor(age).baseCalendar();
        UtcDate today = DateUtil.getUtcRequestDateFor(age);
        requestEvents(today.month(), today.date());
        setUpToolbar();
        initEventsRecycler();

        funnel = new OnThisDayFunnel(WikipediaApp.getInstance(), WikipediaApp.getInstance().getWikiSite(),
                getActivity().getIntent().getIntExtra(OnThisDayActivity.INVOKE_SOURCE_EXTRA, 0));
        return view;
    }

    private void initEventsRecycler() {
        eventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) eventsRecycler
                .getLayoutManager();

       /* eventsRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView,
                                   int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int fullyVisiblePos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                EventsViewHolder view = (EventsViewHolder) eventsRecycler.findViewHolderForLayoutPosition(fullyVisiblePos);
                EventsViewHolder lastView = (EventsViewHolder) eventsRecycler.findViewHolderForLayoutPosition(fullyVisiblePos - 1);
                EventsViewHolder nextView = (EventsViewHolder) eventsRecycler.findViewHolderForLayoutPosition(fullyVisiblePos + 1);

                if (view != null) {
                    if (fullyVisiblePos >= (onThisDay.events().size() - 2)) {
                        backToTopView.setVisibility(View.VISIBLE);
                        if (nextView != null) {
                            nextView.setPadding();
                        }
                    } else {
                        switch (fullyVisiblePos) {
                            case 0:
                                appBarLayout.setExpanded(true);
                                break;
                            case 1:
                                appBarLayout.setExpanded(false);
                                break;
                            default:
                                break;
                        }

                        if (lastView != null) {
                            lastView.hidePadding();
                            lastView.setLightView();

                        }
                        if (nextView != null) {
                            nextView.hidePadding();
                            nextView.setLightView();
                        }
                        backToTopView.setVisibility(View.GONE);
                    }
                    view.setDarkView();
                }
            }
        });*/
    }

    private void requestEvents(String month, String date) {

        new OnThisDayClient().request(wiki, month, date).enqueue(new Callback<OnThisDay>() {
            @Override
            public void onResponse(@NonNull Call<OnThisDay> call, @NonNull Response<OnThisDay> response) {
                if (!isAdded()) {
                    return;
                }
                onThisDay = response.body();
                progressBar.setVisibility(GONE);
                eventsRecycler.setVisibility(View.VISIBLE);
                updateRecyclerView();
                updateTextView();
            }

            @Override
            public void onFailure(@NonNull Call<OnThisDay> call, @NonNull Throwable t) {
                L.e(t);
            }
        });

    }

    private void setUpToolbar() {
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    dayTextView.setVisibility(View.VISIBLE);
                } else if (verticalOffset == 0) {
                    // Expanded
                    dayTextView.setVisibility(GONE);
                }
            }
        });
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    @Override
    public void onDestroyView() {
        if (funnel != null && eventsRecycler.getAdapter() != null) {
            funnel.done(eventsRecycler.getAdapter().getItemCount());
            funnel = null;
        }
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private void updateTextView() {
        if (onThisDay != null) {
            List<OnThisDay.Event> events = onThisDay.events();
            int beginningYear = events.get(events.size() - 1).year();
            dayInfoTextView.setText(String.format(getString(R.string.events_count_text), "" + events.size(),
                    DateUtil.yearToStringWithEra(beginningYear), events.get(0).year()));
        }
        dayTextView.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        dayText.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        calendar.setVisibility(View.VISIBLE);
    }

    private void setUpRecycler(RecyclerView recycler) {
        recycler.addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical));
        recycler.addOnItemTouchListener(new DontInterceptTouchListener());
        recycler.setNestedScrollingEnabled(false);
        recycler.setClipToPadding(false);
        final int padding = DimenUtil.roundedDpToPx(12);
        recycler.setPadding(padding, 0, padding, 0);
    }

    private void updateRecyclerView() {
        setUpRecycler(eventsRecycler);
        if (onThisDay != null) {
            eventsRecycler.setAdapter(new RecyclerAdapter(onThisDay.events(), wiki));
            eventsRecycler.setOnFlingListener(null);
        }
    }

    @Override
    public void onDatePicked(int year, int month, int day) {

        eventsRecycler.setVisibility(GONE);
        progressBar.setVisibility(View.VISIBLE);
        date.set(year, month, day, 0, 0);
        requestEvents("" + (month + 1), "" + day);

    }

    private class RecyclerAdapter extends RecyclerView.Adapter<EventsViewHolder> {
        private List<OnThisDay.Event> events;
        private WikiSite wiki;

        RecyclerAdapter(List<OnThisDay.Event> events, WikiSite wiki) {
            this.wiki = wiki;
            this.events = events;
        }

        @Override
        public EventsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.view_events_layout, viewGroup, false);
            return new EventsViewHolder(itemView, wiki);
        }

        @Override
        public void onBindViewHolder(EventsViewHolder eventsViewHolder, int i) {
            eventsViewHolder.setFields(events.get(i));
            if (funnel != null) {
                funnel.scrolledToPosition(i);
            }
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

    }

    private class EventsViewHolder extends RecyclerView.ViewHolder {
        private View space;
        private TextView descTextView;
        private TextView yearTextView;
        private TextView yearsInfoTextView;
        private RecyclerView pagesRecycler;
        private WikiSite wiki;

        EventsViewHolder(View v, WikiSite wiki) {
            super(v);
            space = v.findViewById(R.id.space);
            descTextView = v.findViewById(R.id.text);
            yearTextView = v.findViewById(R.id.year);
            yearsInfoTextView = v.findViewById(R.id.years_text);
            pagesRecycler = v.findViewById(R.id.pages_recycler);
            this.wiki = wiki;
            setRecycler();
        }

        private void setRecycler() {
            if (pagesRecycler != null) {
                pagesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                setUpRecycler(pagesRecycler);
            }
        }

        public void setFields(final OnThisDay.Event event) {
            setPagesRecycler(event);
            descTextView.setText(event.text());
            yearTextView.setText(DateUtil.yearToStringWithEra(event.year()));
            yearsInfoTextView.setText(DateUtil.getYearDifferenceString(event.year()));
            yearsInfoTextView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.button_shape_light_gray_corner_rounded));
        }


        private void setPagesRecycler(OnThisDay.Event event) {
            if (event.pages() != null) {
                pagesRecycler.setAdapter(new OnThisDayCardView.RecyclerAdapter(event.pages(), wiki, false));
            } else {
                pagesRecycler.setVisibility(GONE);
            }
        }

        void setDarkView() {
            yearsInfoTextView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.button_shape_gray_corner_rounded));
        }

        void setLightView() {
            yearsInfoTextView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.button_shape_light_gray_corner_rounded));
        }

        void setPadding() {
            space.setVisibility(View.VISIBLE);
        }

        void hidePadding() {
            space.setVisibility(GONE);
        }
    }

    @OnClick(R.id.back_to_top_view)
    public void onBackToTopTextViewClicked() {
        if (eventsRecycler != null) {
            eventsRecycler.post(new Runnable() {
                @Override
                public void run() {
                    nestedScrollView.scrollTo(0, 0);
                    appBarLayout.setExpanded(true);
                }
            });
        }
    }

    @OnClick(R.id.calendar)
    public void onCalendarClicked() {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setCallback(OnThisDayFragment.this);
        newFragment.show(getFragmentManager(), "date picker");
    }
}
