package org.wikipedia.page;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Intercept web links and add special behavior for external links.
 */
public class LinkMovementMethodExt extends LinkMovementMethod {
    private UrlHandler handler;

    public LinkMovementMethodExt(UrlHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onTouchEvent(final TextView widget, final Spannable buffer, final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            final int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX();
            final int y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();
            final Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            final int off = layout.getOffsetForHorizontal(line, x);
            final URLSpan[] links = buffer.getSpans(off, off, URLSpan.class);
            if (links.length != 0) {
                try {
                    String url = URLDecoder.decode(links[0].getURL(), "utf-8");
                    handler.onUrlClick(url);
                    return true;
                } catch (UnsupportedEncodingException e) {
                    // won't happen
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }


    public interface UrlHandler {
        void onUrlClick(String url);
    }
}