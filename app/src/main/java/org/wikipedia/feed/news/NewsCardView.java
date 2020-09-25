package org.wikipedia.feed.news;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.views.PositionAwareFragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewsCardView extends DefaultFeedCardView<NewsCard> {
    @BindView(R.id.news_pager) ViewPager2 newsPager;
    @BindView(R.id.header_view) CardHeaderView headerView;
    @BindView(R.id.rtl_container) View rtlContainer;
    @BindView(R.id.news_item_indicator_view) TabLayout newItemIndicatorView;
    @Nullable private AppCompatActivity activity;

    public interface Callback {
        void onNewsItemSelected(@NonNull NewsCard card, NewsCardItemFragment view);
    }

    public NewsCardView(@NonNull Context context) {
        super(context);
        View view = inflate(getContext(), R.layout.view_card_news, this);
        activity = (AppCompatActivity) view.getContext();
        ButterKnife.bind(this);
    }

    @Override
    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    @Override public void setCard(@NonNull NewsCard card) {
        super.setCard(card);
        header(card);
        setLayoutDirectionByWikiSite(card.wikiSite(), rtlContainer);
        if (activity != null) {
            newsPager.setOffscreenPageLimit(2);
            newsPager.setAdapter(new NewsAdapter(activity, card));
        }
        new TabLayoutMediator(newItemIndicatorView, newsPager, (tab, position) -> tab.view.setClickable(false)).attach();
    }

    private void header(@NonNull NewsCard card) {
        headerView.setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card);
    }

    private class NewsAdapter extends PositionAwareFragmentStateAdapter {
        private List<NewsItem> newsItems = new ArrayList<>();
        private NewsCard card;

        NewsAdapter(@NonNull AppCompatActivity activity, @NonNull NewsCard card) {
            super(activity);
            this.card = card;
            newsItems.addAll(card.news());
        }


        @NonNull @Override public Fragment createFragment(int position) {
            NewsCardItemFragment fragment = NewsCardItemFragment.newInstance(newsItems.get(position), card);
            fragment.setCallback(getCallback());
            return fragment;
        }

        @Override
        public int getItemCount() {
            return newsItems.size();
        }
    }
}
