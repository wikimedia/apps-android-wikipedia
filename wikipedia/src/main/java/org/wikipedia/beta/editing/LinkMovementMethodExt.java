package org.wikipedia.beta.editing;

import android.content.Context;
import android.net.Uri;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;
import org.wikipedia.beta.Utils;

/**
 * Intercept web links and add special behavior for external links.
 * Currently treats any link as external link.
 */
public class LinkMovementMethodExt extends LinkMovementMethod {
    private Context context;

    LinkMovementMethodExt(Context context) {
        this.context = context;
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
            final ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);
            if (links.length != 0) {
                ClickableSpan link = links[0];
                if (link instanceof URLSpan) {
                    String url = ((URLSpan) link).getURL();
                    Utils.handleExternalLink(context, Uri.parse(url));
                    return true;
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }
}