package org.wikipedia.feed.news;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.L10nUtil.isLangRTL;

public class NewsCardView extends DefaultFeedCardView<NewsCard> {
    @BindView(R.id.news_recycler_view) RecyclerView newsRecyclerView;
    @BindView(R.id.header_view) CardHeaderView headerView;
    @BindView(R.id.rtl_container) View rtlContainer;
    private boolean isSnapHelperAttached;

    public interface Callback {
        void onNewsItemSelected(@NonNull NewsCard card, NewsItemView view);
    }

    public NewsCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_news, this);
        ButterKnife.bind(this);
    }

    private void setUpIndicatorDots() {
        final int indicatorRadius = 4;
        final int indicatorPadding = 8;
        final int indicatorHeight = 20;
        newsRecyclerView.addItemDecoration(new RecyclerViewIndicatorDotDecor(DimenUtil.roundedDpToPx(indicatorRadius),
                DimenUtil.roundedDpToPx(indicatorPadding), DimenUtil.roundedDpToPx(indicatorHeight),
                ResourceUtil.getThemedColor(getContext(), R.attr.chart_shade5),
                ResourceUtil.getThemedColor(getContext(), R.attr.colorAccent), isLangRTL(getCard().wikiSite().languageCode())));
    }

    @Override
    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    @Override public void setCard(@NonNull NewsCard card) {
        if (card.equals(getCard())) {
            return;
        }
        super.setCard(card);
        header(card);
        setLayoutDirectionByWikiSite(card.wikiSite(), rtlContainer);
        setUpRecycler(card);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setUpRecycler(NewsCard card) {
        newsRecyclerView.setHasFixedSize(true);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        newsRecyclerView.setNestedScrollingEnabled(false);
        newsRecyclerView.setClipToPadding(false);
        newsRecyclerView.setAdapter(new NewsAdapter(card));
        setUpIndicatorDots();
        setUpSnapHelper();
    }

    private void setUpSnapHelper() {
        if (!isSnapHelperAttached) {
            final SnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(newsRecyclerView);
            isSnapHelperAttached = true;
        }
    }

    private void header(@NonNull NewsCard card) {
        headerView.setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card);
    }

    private static class NewsItemHolder extends RecyclerView.ViewHolder {
        NewsItemView itemView;

        NewsItemHolder(NewsItemView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        void bindItem(NewsItem newsItem) {
            itemView.setContents(newsItem);
        }

        NewsItemView getView() {
            return itemView;
        }
    }

    private class NewsAdapter extends RecyclerView.Adapter<NewsItemHolder> {
        private List<NewsItem> newsItems = new ArrayList<>();
        private NewsCard card;

        NewsAdapter(@NonNull NewsCard card) {
            this.card = card;
            this.newsItems.addAll(card.news());
        }

        @Override
        public int getItemCount() {
            return newsItems.size();
        }

        @NonNull
        @Override
        public NewsItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NewsItemHolder(new NewsItemView(getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull NewsItemHolder holder, int position) {
            holder.bindItem(newsItems.get(position));
            holder.getView().setOnClickListener(view -> {
                if (getCallback() != null) {
                    getCallback().onNewsItemSelected(card, holder.getView());
                }
            });
        }

        @Override
        public void onViewAttachedToWindow(@NonNull NewsItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(getCallback());
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull NewsItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }
}
