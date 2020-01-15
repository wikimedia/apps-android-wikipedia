package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.FeedFunnel;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.MarginItemDecoration;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_BODY;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_FOOTER;

public class OnThisDayCardView extends DefaultFeedCardView<OnThisDayCard> implements ItemTouchHelperSwipeAdapter.SwipeableView, OnThisDayActionsDialog.Callback {
    @BindView(R.id.view_on_this_day_card_header) CardHeaderView headerView;
    @BindView(R.id.text) TextView descTextView;
    @BindView(R.id.next_event_years) TextView nextEventYearsTextView;
    @BindView(R.id.day) TextView dayTextView;
    @BindView(R.id.year) TextView yearTextView;
    @BindView(R.id.years_text_background) ImageView yearsInfoBackground;
    @BindView(R.id.years_text) TextView yearsInfoTextView;
    @BindView(R.id.year_layout) LinearLayout yearLayout;
    @BindView(R.id.more_events_layout) LinearLayout moreEventsLayout;
    @BindView(R.id.pages_recycler) RecyclerView pagesRecycler;
    @BindView(R.id.gradient_layout) View gradientLayout;
    @BindView(R.id.radio_image_view) View radio;
    @BindView(R.id.view_on_this_day_rtl_container) View rtlContainer;
    private FeedFunnel funnel = new FeedFunnel(WikipediaApp.getInstance());

    private int age;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    public OnThisDayCardView(@NonNull Context context) {
        super(context);
        setAllowOverflow(true);
        inflate(getContext(), R.layout.view_card_on_this_day, this);
        ButterKnife.bind(this);
        initRecycler();
        setGradientAndTextColor();
    }

    private void setGradientAndTextColor() {
        gradientLayout.setBackground(GradientUtil.getPowerGradient(ResourceUtil.getThemedAttributeId(getContext(), R.attr.chart_shade5), Gravity.BOTTOM));
        DrawableCompat.setTint(yearsInfoBackground.getDrawable(), ResourceUtil.getThemedColor(getContext(), R.attr.secondary_text_color));
    }

    private void initRecycler() {
        pagesRecycler.setHasFixedSize(true);
        pagesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        pagesRecycler.addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical));
        pagesRecycler.addOnItemTouchListener(new DontInterceptTouchListener());
        pagesRecycler.setNestedScrollingEnabled(false);
    }

    @Override
    public void onAddPageToList(@NonNull HistoryEntry entry) {
        bottomSheetPresenter.show(((AppCompatActivity) getContext()).getSupportFragmentManager(),
                AddToReadingListDialog.newInstance(entry.getTitle(), ON_THIS_DAY_ACTIVITY));
    }

    @Override
    public void onSharePage(@NonNull HistoryEntry entry) {
        ShareUtil.shareText(getContext(), entry.getTitle());
    }

    static class RecyclerAdapter extends RecyclerView.Adapter<OnThisDayPagesViewHolder> {
        private List<PageSummary> pages;
        private WikiSite wiki;
        private final boolean isSingleCard;
        private OnThisDayPagesViewHolder.ItemCallBack itemCallback;

        RecyclerAdapter(@NonNull List<PageSummary> pages, @NonNull WikiSite wiki, boolean isSingleCard) {
            this.pages = pages;
            this.wiki = wiki;
            this.isSingleCard = isSingleCard;
        }

        public RecyclerAdapter setCallback(OnThisDayPagesViewHolder.ItemCallBack itemCallBack) {
            this.itemCallback = itemCallBack;
            return this;
        }

        @NonNull @Override
        public OnThisDayPagesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.item_on_this_day_pages, viewGroup, false);
            return new OnThisDayPagesViewHolder((Activity) viewGroup.getContext(), (CardView) itemView, wiki, isSingleCard);
        }

        @Override
        public void onBindViewHolder(@NonNull OnThisDayPagesViewHolder onThisDayPagesViewHolder, int i) {
            if (itemCallback != null) {
                onThisDayPagesViewHolder
                        .setCallback(itemCallback)
                        .setFields(pages.get(i));
            }
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    @Override
    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    private void header(@NonNull OnThisDayCard card) {
        headerView.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_otd_icon)
                .setImageCircleColor(ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent))
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());
        descTextView.setText(card.text());
        yearTextView.setText(DateUtil.yearToStringWithEra(card.year()));
        yearsInfoTextView.setText(DateUtil.getYearDifferenceString(card.year()));
        dayTextView.setText(card.dayString());
        nextEventYearsTextView.setText(DateUtil.getYearDifferenceString(card.nextYear()));
    }

    @Override
    public void setCard(@NonNull OnThisDayCard card) {
        super.setCard(card);
        this.age = card.getAge();
        setLayoutDirectionByWikiSite(card.wikiSite(), rtlContainer);
        setPagesRecycler(card);
        header(card);
    }

    @OnClick({R.id.view_on_this_day_click_container}) void onMoreClick() {
        funnel.cardClicked(CardType.ON_THIS_DAY, getCard().wikiSite().languageCode());
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation((Activity) getContext(), dayTextView, getContext().getString(R.string.transition_on_this_day));
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), age, getCard().wikiSite(),
                ON_THIS_DAY_CARD_BODY), options.toBundle());
    }

    @OnClick({R.id.more_events_layout}) void onMoreFooterClick() {
        funnel.cardClicked(CardType.ON_THIS_DAY, getCard().wikiSite().languageCode());
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation((Activity) getContext(), dayTextView, getContext().getString(R.string.transition_on_this_day));
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), age, getCard().wikiSite(),
                ON_THIS_DAY_CARD_FOOTER), options.toBundle());
    }

    private void setPagesRecycler(OnThisDayCard card) {
        if (card.pages() != null) {
            RecyclerAdapter recyclerAdapter = new RecyclerAdapter(card.pages(), card.wikiSite(), true);
            recyclerAdapter.setCallback(new ItemCallback());
            pagesRecycler.setAdapter(recyclerAdapter);
        } else {
            pagesRecycler.setVisibility(GONE);
        }
    }

    class ItemCallback implements OnThisDayPagesViewHolder.ItemCallBack {
        @Override
        public void onActionLongClick(@NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(((AppCompatActivity)getContext()).getSupportFragmentManager(),
                    OnThisDayActionsDialog.newInstance(entry));
        }
    }
}
