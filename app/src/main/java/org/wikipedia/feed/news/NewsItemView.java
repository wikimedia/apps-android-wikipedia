package org.wikipedia.feed.news;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.WikiCardView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class NewsItemView extends WikiCardView {
    @BindView(R.id.horizontal_scroll_list_item_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.horizontal_scroll_list_item_text) TextView textView;
    @Nullable private FeedAdapter.Callback callback;
    @Nullable private NewsItem newsItem;

    public NewsItemView(@NonNull Context context) {
        super(context);
        View view = inflate(getContext(), R.layout.view_horizontal_scroll_list_item_card, this);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ButterKnife.bind(this);
    }

    public void setContents(@NonNull NewsItem newsItem) {
        this.newsItem = newsItem;
        setText(defaultString(newsItem.story()));
        setImage(newsItem.thumb());
    }

    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    @Nullable
    public FeedAdapter.Callback getCallback() {
        return callback;
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

    @Nullable
    public NewsItem getNewsItem() {
        return newsItem;
    }
}
