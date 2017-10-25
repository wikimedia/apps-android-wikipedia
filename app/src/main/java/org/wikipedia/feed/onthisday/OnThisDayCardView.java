package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OnThisDayCardView extends DefaultFeedCardView<OnThisDayCard> {
    @BindView(R.id.view_on_this_day_card_header) View headerView;
    @BindView(R.id.text) TextView descTextView;
    @BindView(R.id.year) TextView yearTextView;
    @BindView(R.id.next_year) TextView nextYearTextView;
    @BindView(R.id.years_text) TextView yearsInfoTextView;
    @BindView(R.id.year_layout) LinearLayout yearLayout;
    @BindView(R.id.next_year_layout) LinearLayout nextYearLayout;
    @BindView(R.id.more_events_layout) LinearLayout moreEventsLayout;
    @BindView(R.id.pages_recycler) RecyclerView pagesRecycler;
    private WikiSite wiki;
    private UtcDate date;
    private OnThisDay onThisDay;

    public OnThisDayCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_on_this_day, this);
        ButterKnife.bind(this);
        initRecycler();
    }

    private void launchOnThisDayActivity() {
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), onThisDay, wiki, date));
    }

    private void initRecycler() {
        pagesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        pagesRecycler.addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical));
        pagesRecycler.addOnItemTouchListener(new DontInterceptTouchListener());
        pagesRecycler.setNestedScrollingEnabled(false);
    }

    static class RecyclerAdapter extends RecyclerView.Adapter<OnThisDayPagesViewHolder> {
        private List<OnThisDay.Page> pages;
        private WikiSite wiki;

        RecyclerAdapter(@NonNull List<OnThisDay.Page> pages, WikiSite wiki) {
            this.pages = pages;
            this.wiki = wiki;
        }

        @Override
        public OnThisDayPagesViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.item_on_this_day_pages, viewGroup, false);
            return new OnThisDayPagesViewHolder((CardView) itemView, wiki);
        }

        @Override
        public void onBindViewHolder(OnThisDayPagesViewHolder onThisDayPagesViewHolder, int i) {
            onThisDayPagesViewHolder.setFields(pages.get(i));
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    @Override
    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        if (headerView instanceof CardHeaderView) {
            ((CardHeaderView) headerView).setCallback(callback);
        }
    }

    private void header(@NonNull OnThisDayCard card) {

        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_otd_icon)
                .setImageCircleColor(R.color.base30)
                .setCard(card)
                .setCallback(getCallback());
        descTextView.setText(card.text());
        yearTextView.setText(DateUtil.yearToStringWithEra(card.year()));
        yearsInfoTextView.setText(DateUtil.getYearDifferenceString(card.year()));
        yearLayout.setBackground(GradientUtil.getPowerGradient(R.color.base100, Gravity.TOP));
        nextYearLayout.setBackground(GradientUtil.getPowerGradient(R.color.base100, Gravity.BOTTOM));
        nextYearTextView.setText(DateUtil.yearToStringWithEra(card.nextYear()));
        this.wiki = card.wiki();
        this.onThisDay = card.onthisday();
        this.date = card.date();
        header(header);
    }

    private void header(@NonNull View view) {
        ViewUtil.replace(headerView, view);
        headerView = view;
    }

    @Override
    public void setCard(@NonNull OnThisDayCard card) {
        super.setCard(card);
        pagesRecycler.setAdapter(new RecyclerAdapter(card.pages(), card.wiki()));
        header(card);
    }

    @OnClick({R.id.more_events_layout, R.id.view_on_this_day_click_container}) void onMoreClick() {
        launchOnThisDayActivity();
    }
}
