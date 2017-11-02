package org.wikipedia.feed.onthisday;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.wikipedia.feed.onthisday.OnThisDayActivity.AGE;

public class OnThisDayFragment extends Fragment {
    @BindView(R.id.day_text_view) TextView dayTextView;
    @BindView(R.id.day_info_text_view) TextView dayInfoTextView;
    @BindView(R.id.events_recycler) RecyclerView eventsRecycler;
    @BindView(R.id.progress) ProgressBar progressBar;
    @Nullable private OnThisDay onThisDay;
    @Nullable private WikiSite wiki;
    @Nullable private UtcDate date;
    private Unbinder unbinder;

    @NonNull public static OnThisDayFragment newInstance(int age) {

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

        new OnThisDayClient().request(wiki, age).enqueue(new Callback<OnThisDay>() {
            @Override
            public void onResponse(@NonNull Call<OnThisDay> call, @NonNull Response<OnThisDay> response) {
                if (!isAdded()) {
                    return;
                }
                onThisDay = response.body();
                date = DateUtil.getUtcRequestDateFor(age);
                progressBar.setVisibility(View.GONE);
                updateTextView();
                updateRecyclerView();
            }

            @Override
            public void onFailure(@NonNull Call<OnThisDay> call, @NonNull Throwable t) {
                L.e(t);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
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

        dayTextView.setText(new SimpleDateFormat("MMMM d", Locale.getDefault()).format(date.baseCalendar().getTime()));
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
        eventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        setUpRecycler(eventsRecycler);
        if (onThisDay != null) {
            eventsRecycler.setAdapter(new RecyclerAdapter(onThisDay.events(), wiki));
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

        EventsViewHolder(View v, WikiSite wiki) {
            super(v);
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
            pagesRecycler.setAdapter(new OnThisDayCardView.RecyclerAdapter(event.pages(), wiki));
            descTextView.setText(event.text());
            yearTextView.setText(DateUtil.yearToStringWithEra(event.year()));
            yearsInfoTextView.setText(DateUtil.getYearDifferenceString(event.year()));
        }
    }

}
