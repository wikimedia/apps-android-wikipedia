package org.wikipedia.feed.news;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewsCardView extends DefaultFeedCardView<NewsCard> {
    @BindView(R.id.news_pager) ViewPager2 newsPager;
    @BindView(R.id.header_view) CardHeaderView headerView;
    @BindView(R.id.rtl_container) View rtlContainer;
    @BindView(R.id.news_item_indicator_view)
    TabLayout newItemIndicatorView;

    public interface Callback {
        void onNewsItemSelected(@NonNull NewsItem card, ImageView transitionView);
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
    private class ViewHolder extends RecyclerView.ViewHolder {
        private FaceAndColorDetectImageView faceAndColorDetectImageView;
        private TextView text;

        ViewHolder(View itemView) {
            super(itemView);
            faceAndColorDetectImageView = itemView.findViewById(R.id.horizontal_scroll_list_item_image);
            text = itemView.findViewById(R.id.horizontal_scroll_list_item_text);
        }

        void bindItem(NewsItem newsItem) {
            if (newsItem.thumb() == null) {
                faceAndColorDetectImageView.setVisibility(GONE);
                text.setMaxLines(10);
            } else {
                faceAndColorDetectImageView.setVisibility(VISIBLE);
                faceAndColorDetectImageView.loadImage(newsItem.thumb());
            }
            text.setText(removeImageCaption(StringUtil.fromHtml(newsItem.story())));
            itemView.setOnClickListener((view) -> {
                if (getCallback() != null) {
                    getCallback().onNewsItemSelected(newsItem, faceAndColorDetectImageView);
                }
            });
        }

        /* Remove the in-Wikitext thumbnail caption, which will almost certainly not apply here */
        @NonNull
        private CharSequence removeImageCaption(@NonNull Spanned text) {
            Object[] spans = RichTextUtil.getSpans(text, 0, text.length());
            for (Object span : spans) {
                if (span instanceof StyleSpan && ((StyleSpan) span).getStyle() == Typeface.ITALIC) {
                    int start = text.getSpanStart(span);
                    int end = text.getSpanEnd(span);
                    if (text.charAt(start) == '(' && text.charAt(end - 1) == ')') {
                        L.v("Removing spanned text: " + text.subSequence(start, end));
                        return RichTextUtil.remove(text, start, end);
                    }
                }
            }
            return text;
        }
    }
    private class NewsAdapter extends RecyclerView.Adapter {
        private List<NewsItem> newsItems = new ArrayList<>();

        NewsAdapter(NewsCard card) {
            this.newsItems.addAll(card.news());
        }

        @Override
        public int getItemCount() {
            return newsItems.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.view_horizontal_scroll_list_item_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((ViewHolder) holder).bindItem(newsItems.get(position));
        }
    }
}
