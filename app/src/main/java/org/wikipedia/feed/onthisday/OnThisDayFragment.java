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
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.feed.onthisday.OnThisDayActivity.EXTRA_DATE;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.EXTRA_PAGES;
import static org.wikipedia.feed.onthisday.OnThisDayActivity.EXTRA_WIKI;

public class OnThisDayFragment extends Fragment {
    @BindView(R.id.day_text_view) TextView dayTextView;
    @BindView(R.id.day_info_text_view) TextView dayInfoTextView;
    @BindView(R.id.events_recycler) RecyclerView eventsRecycler;
    private OnThisDay onThisDay;
    private WikiSite wiki;
    private UtcDate date;
    private Unbinder unbinder;

    @NonNull
    public static OnThisDayFragment newInstance(@NonNull OnThisDay onThisDay, @NonNull WikiSite wiki, String stringExtra) {
        OnThisDayFragment instance = new OnThisDayFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_PAGES, GsonMarshaller.marshal(onThisDay));
        args.putString(EXTRA_WIKI, GsonMarshaller.marshal(wiki));
        args.putString(EXTRA_DATE, stringExtra);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        onThisDay = GsonUnmarshaller.unmarshal(OnThisDay.class, getActivity().getIntent().getStringExtra(EXTRA_PAGES));
        wiki = GsonUnmarshaller.unmarshal(WikiSite.class, getActivity().getIntent().getStringExtra(EXTRA_WIKI));
        date = GsonUnmarshaller.unmarshal(UtcDate.class, getActivity().getIntent().getStringExtra(EXTRA_DATE));
        View view = inflater.inflate(R.layout.fragment_on_this_day, container, false);
        unbinder = ButterKnife.bind(this, view);
        updateTextView();
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

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        eventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        setUpRecycler(eventsRecycler);
        if (onThisDay != null) {
            eventsRecycler.setAdapter(new RecyclerAdapter(onThisDay.events(), wiki));
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
        final int padding = DimenUtil.roundedDpToPx(12);
        recycler.setPadding(padding, 0, padding, 0);
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
