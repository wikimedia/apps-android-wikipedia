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
import org.wikipedia.util.log.L;

import static org.wikipedia.util.UriUtil.decodeURL;

/**
 * Intercept web links and add special behavior for external links.
 */
public class LinkMovementMethodExt extends LinkMovementMethod {
    @Nullable private UrlHandler handler;
    @Nullable private UrlHandlerWithText handlerWithText;

    public LinkMovementMethodExt(@Nullable UrlHandler handler) {
        this.handler = handler;
    }

    public LinkMovementMethodExt(@Nullable UrlHandlerWithText handler) {
        this.handlerWithText = handler;
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
                String linkText;
                try {
                    linkText = buffer.subSequence(buffer.getSpanStart(links[0]), buffer.getSpanEnd(links[0])).toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    linkText = "";
                }
                L.d(linkText);
                String url = decodeURL(links[0].getURL());
                if (handler != null) {
                    handler.onUrlClick(url);
                } else if (handlerWithText != null) {
                    handlerWithText.onUrlClick(url, UriUtil.getTitleFromUrl(url), linkText);
                }
                return true;
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }

    public interface UrlHandler {
        void onUrlClick(@NonNull String url);
    }

    public interface UrlHandlerWithText {
        void onUrlClick(@NonNull String url, @Nullable String titleString, @NonNull String linkText);
    }
}
