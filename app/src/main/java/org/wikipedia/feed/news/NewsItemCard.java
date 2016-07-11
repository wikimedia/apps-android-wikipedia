package org.wikipedia.feed.news;

import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.StyleSpan;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.log.L;

import java.util.List;

public class NewsItemCard extends Card {
    @NonNull private NewsItem newsItem;
    @NonNull private Site site;

    public NewsItemCard(@NonNull NewsItem item, @NonNull Site site) {
        this.newsItem = item;
        this.site = site;
    }

    @NonNull
    public NewsItem item() {
        return newsItem;
    }

    @NonNull
    public Site site() {
        return site;
    }

    @Nullable
    @Override
    public Uri image() {
        return newsItem.thumb();
    }

    @NonNull
    public CharSequence text() {
        return removeImageCaption(Html.fromHtml(newsItem.story()));
    }

    public List<CardPageItem> links() {
        return newsItem.links();
    }

    // Unused
    @NonNull
    @Override
    public String title() {
        return "";
    }

    /* Remove the in-Wikitext thumbnail caption, which will almost certainly not apply here */
    private CharSequence removeImageCaption(Spanned text) {
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
