package org.wikipedia.feed.news;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.apache.commons.lang3.StringUtils.defaultString;


public class NewsCardItemFragment extends Fragment {
    @BindView(R.id.news_card_list_item_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.news_card_list_item_text) TextView textView;
    @Nullable private FeedAdapter.Callback callback;
    private Unbinder unbinder;
    public static final String EXTRA_NEWS_ITEM = "item";
    public static final String EXTRA_NEWS_CARD = "card";
    public static final int MARGIN = 8;
    @Nullable private NewsItem newsItem;
    @Nullable private NewsCard card;


    public static NewsCardItemFragment newInstance(@NonNull NewsItem newsItem, @NonNull NewsCard newsCard) {
        NewsCardItemFragment instance = new NewsCardItemFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_NEWS_ITEM, GsonMarshaller.marshal(newsItem));
        args.putString(EXTRA_NEWS_CARD, GsonMarshaller.marshal(newsCard));
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            newsItem = GsonUnmarshaller.unmarshal(NewsItem.class, getArguments().getString(EXTRA_NEWS_ITEM));
            card = GsonUnmarshaller.unmarshal(NewsCard.class, getArguments().getString(EXTRA_NEWS_CARD));
        }
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_news_card_view_item, container, false);
        unbinder = ButterKnife.bind(this, view);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int margin = (int) DimenUtil.dpToPx(MARGIN);
        layoutParams.setMargins(margin, margin, margin, margin);
        view.setLayoutParams(layoutParams);
        setContents();
        return view;
    }

    public void setContents() {
        if (newsItem != null) {
            setText(defaultString(newsItem.story()));
            setImage(newsItem.thumb());
        }
    }

    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    @Nullable
    public FeedAdapter.Callback getCallback() {
        return callback;
    }

    @OnClick(R.id.news_card_list_item_base) void onItemClick() {
        if (card != null && callback != null) {
            callback.onNewsItemSelected(card, this);
        }
    }

    public void setText(@NonNull String text) {
        textView.setText(removeImageCaption(StringUtil.fromHtml(text)));
        RichTextUtil.removeUnderlinesFromLinksAndMakeBold(textView);
    }

    public void setImage(@Nullable Uri image) {
        if (image == null) {
            imageView.setVisibility(GONE);
            textView.setMaxLines(10);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView.loadImage(image);
        }
    }

    public View getImageView() {
        return imageView;
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

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Nullable
    public NewsItem getNewsItem() {
        return newsItem;
    }
}
