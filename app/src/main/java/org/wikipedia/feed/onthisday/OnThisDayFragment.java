package org.wikipedia.feed.onthisday;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
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
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.log.L;
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

public class OnThisDayFragment extends Fragment {
    @BindView(R.id.day) TextView dayText;
    @BindView(R.id.day_text_view) TextView dayTextView;
    @BindView(R.id.day_info_text_view) TextView dayInfoTextView;
    @BindView(R.id.events_recycler) RecyclerView eventsRecycler;
    @BindView(R.id.progress) ProgressBar progressBar;
    @BindView(R.id.back_to_top_view) RelativeLayout backToTopView;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.nested) NestedScrollView nestedScrollView;
    @BindView(R.id.linear_layout) LinearLayout linearLayout;
    @BindView(R.id.space) LinearLayout space;
    @BindView(R.id.calendar) ImageView calendar;
    @BindView(R.id.upward_arrow) ImageView upwardArrow;
    @Nullable private OnThisDay onThisDay;
    @Nullable private WikiSite wiki;
    private Calendar date;
    private Unbinder unbinder;
    @Nullable private OnThisDayFunnel funnel;
    public static final int PADDING1 = 24, PADDING2 = 38, PADDING3 = 23;

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
        OnThisDayFragment.this.date = DateUtil.getDefaultDateFor(age);
        setUpToolbar();
        Calendar today = DateUtil.getDefaultDateFor(age);
        requestEvents(today.get(Calendar.MONTH), today.get(Calendar.DATE));
        funnel = new OnThisDayFunnel(WikipediaApp.getInstance(), WikipediaApp.getInstance().getWikiSite(),
                getActivity().getIntent().getIntExtra(OnThisDayActivity.INVOKE_SOURCE_EXTRA, 0));
        return view;
    }

    private void requestEvents(int month, int date) {

        new OnThisDayClient().request(wiki, month + 1, date).enqueue(new Callback<OnThisDay>() {
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
        dayTextView.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        dayText.setText(DateUtil.getMonthOnlyDateString(date.getTime()));
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    dayTextView.setVisibility(View.VISIBLE);
                } else if (verticalOffset == 0) {
                    // Expanded
                    dayTextView.setVisibility(View.GONE);
                } else {
                    // In Transition
                    dayTextView.setVisibility(View.GONE);
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
    }

    private void updateRecyclerView() {
        eventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        setUpRecycler(eventsRecycler);
        if (onThisDay != null) {
            eventsRecycler.setAdapter(new RecyclerAdapter(onThisDay.events(), wiki));
            eventsRecycler.setOnFlingListener(null);
            backToTopView.setVisibility(View.VISIBLE);
            space.setVisibility(View.VISIBLE);
            calendar.setVisibility(View.VISIBLE);
        }
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
        private TextView descTextView;
        private TextView yearTextView;
        private TextView yearsInfoTextView;
        private RecyclerView pagesRecycler;
        private WikiSite wiki;
        private View radio;

        EventsViewHolder(View v, WikiSite wiki) {
            super(v);
            descTextView = v.findViewById(R.id.text);
            yearTextView = v.findViewById(R.id.year);
            yearsInfoTextView = v.findViewById(R.id.years_text);
            pagesRecycler = v.findViewById(R.id.pages_recycler);
            radio = v.findViewById(R.id.radio_image_view);
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
            setPads();
            descTextView.setText(event.text());
            yearTextView.setText(DateUtil.yearToStringWithEra(event.year()));
            yearsInfoTextView.setText(DateUtil.getYearDifferenceString(event.year()));
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
                pagesRecycler.setAdapter(new OnThisDayCardView.RecyclerAdapter(event.pages(), wiki, false));
            } else {
                pagesRecycler.setVisibility(GONE);
            }
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

}
