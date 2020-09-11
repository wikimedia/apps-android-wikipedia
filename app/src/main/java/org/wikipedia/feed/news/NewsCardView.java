package org.wikipedia.feed.news;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewsCardView extends DefaultFeedCardView<NewsCard> {
    @BindView(R.id.news_pager) ViewPager2 newsPager;
    @BindView(R.id.header_view) CardHeaderView headerView;
    @BindView(R.id.rtl_container) View rtlContainer;
    @BindView(R.id.news_item_indicator_view) TabLayout newItemIndicatorView;

    public interface Callback {
        void onNewsItemSelected(@NonNull NewsCard card, NewsItemView view);
    }

    public NewsCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_news, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull NewsCard card) {
        super.setCard(card);
        header(card);
        setLayoutDirectionByWikiSite(card.wikiSite(), rtlContainer);
        newsPager.setOffscreenPageLimit(2);
        newsPager.setAdapter(new NewsAdapter(card));
        new TabLayoutMediator(newItemIndicatorView, newsPager, (tab, position) -> { }).attach();
    }

    private void header(@NonNull NewsCard card) {
        headerView.setTitle(R.string.view_card_news_title)
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());
    }

    private class NewsItemHolder extends RecyclerView.ViewHolder {
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

    private class NewsAdapter extends RecyclerView.Adapter {
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
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NewsItemHolder(new NewsItemView(getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((NewsItemHolder) holder).bindItem(newsItems.get(position));
            ((NewsItemHolder) holder).getView().setOnClickListener(view -> {
                if (getCallback() != null) {
                    getCallback().onNewsItemSelected(card, ((NewsItemHolder) holder).getView());
                }
            });
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ((NewsItemHolder) holder).getView().setCallback(getCallback());
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
            ((NewsItemHolder) holder).getView().setCallback(null);
        }
    }
}
