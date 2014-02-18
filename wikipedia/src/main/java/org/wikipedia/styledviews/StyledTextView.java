package org.wikipedia.styledviews;

import android.content.*;
import android.util.*;
import android.widget.*;
import org.wikipedia.*;

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
