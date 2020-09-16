package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.FeedFunnel;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DateUtil;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_BODY;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_FOOTER;

public class OnThisDayCardView extends DefaultFeedCardView<OnThisDayCard> implements CardFooterView.Callback {
    @BindView(R.id.view_on_this_day_card_header) CardHeaderView headerView;
    @BindView(R.id.text) TextView descTextView;
    @BindView(R.id.year) TextView yearTextView;
    @BindView(R.id.years_text) TextView yearsInfoTextView;
    @BindView(R.id.year_layout) LinearLayout yearLayout;
    @BindView(R.id.pages_recycler) RecyclerView pagesRecycler;
    @BindView(R.id.radio_image_view) View radio;
    @BindView(R.id.view_on_this_day_rtl_container) View rtlContainer;
    @BindView(R.id.card_footer_view) CardFooterView cardFooterView;
    private FeedFunnel funnel = new FeedFunnel(WikipediaApp.getInstance());

    private int age;

    public OnThisDayCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_on_this_day, this);
        ButterKnife.bind(this);
        initRecycler();
        setUpFooter();
    }

    private void setUpFooter() {
        cardFooterView.setFooterActionText(getContext().getString(R.string.more_events_text));
        cardFooterView.setCallback(this);
    }

    @Override
    public void onFooterClicked() {
        funnel.cardClicked(CardType.ON_THIS_DAY, getCard().wikiSite().languageCode());
        //Todo: add transition
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), age, getCard().wikiSite(),
                ON_THIS_DAY_CARD_FOOTER));
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

    static class RecyclerAdapter extends RecyclerView.Adapter<OnThisDayPagesViewHolder> {
        private List<PageSummary> pages;
        private WikiSite wiki;
        private final boolean isSingleCard;
        private FragmentManager fragmentManager;

        RecyclerAdapter(@NonNull FragmentManager fragmentManager, @NonNull List<PageSummary> pages, @NonNull WikiSite wiki, boolean isSingleCard) {
            this.pages = pages;
            this.wiki = wiki;
            this.isSingleCard = isSingleCard;
            this.fragmentManager = fragmentManager;
        }

        @NonNull @Override
        public OnThisDayPagesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.item_on_this_day_pages, viewGroup, false);
            return new OnThisDayPagesViewHolder((Activity) viewGroup.getContext(), fragmentManager, (MaterialCardView) itemView, wiki, isSingleCard);
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

    @Override
    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    private void header(@NonNull OnThisDayCard card) {
        headerView.setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());
        descTextView.setText(card.text());
        yearTextView.setText(DateUtil.yearToStringWithEra(card.year()));
    }

    @Override
    public void setCard(@NonNull OnThisDayCard card) {
        super.setCard(card);
        this.age = card.getAge();
        setLayoutDirectionByWikiSite(card.wikiSite(), rtlContainer);
        yearsInfoTextView.setText(DateUtil.getYearDifferenceString(card.year()));
        setPagesRecycler(card);
        header(card);
    }

    @OnClick({R.id.view_on_this_day_click_container}) void onMoreClick() {
        funnel.cardClicked(CardType.ON_THIS_DAY, getCard().wikiSite().languageCode());
        //Todo: add transition
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), age, getCard().wikiSite(),
                ON_THIS_DAY_CARD_BODY));
    }

    private void setPagesRecycler(OnThisDayCard card) {
        if (card.pages() != null) {
            RecyclerAdapter recyclerAdapter = new RecyclerAdapter(((AppCompatActivity) getContext()).getSupportFragmentManager(), card.pages(), card.wikiSite(), true);
            pagesRecycler.setAdapter(recyclerAdapter);
        } else {
            pagesRecycler.setVisibility(GONE);
        }
    }
}
