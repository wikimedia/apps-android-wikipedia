package org.wikipedia.styledviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import org.wikipedia.WikipediaApp;

public class StyledTextView extends TextView {
    public StyledTextView(Context context) {
        super(context);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }

    public StyledTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }

    public StyledTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }
}
