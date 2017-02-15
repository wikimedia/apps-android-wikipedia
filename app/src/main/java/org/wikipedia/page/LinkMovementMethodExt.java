package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import org.wikipedia.util.UriUtil;

import static org.wikipedia.util.UriUtil.decodeURL;

/**
 * Intercept web links and add special behavior for external links.
 */
public class LinkMovementMethodExt extends LinkMovementMethod {
    private UrlHandler handler;

    public LinkMovementMethodExt(UrlHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onTouchEvent(@NonNull final TextView widget, @NonNull final Spannable buffer, @NonNull final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            final int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX();
            final int y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();
            final Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            final int off = layout.getOffsetForHorizontal(line, x);
            final URLSpan[] links = buffer.getSpans(off, off, URLSpan.class);
            if (links.length != 0) {
                String url = decodeURL(links[0].getURL());
                handler.onUrlClick(url, UriUtil.getTitleFromUrl(url));
                return true;
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }


    public interface UrlHandler {
        void onUrlClick(@NonNull String url, @Nullable String titleString);
    }
}
