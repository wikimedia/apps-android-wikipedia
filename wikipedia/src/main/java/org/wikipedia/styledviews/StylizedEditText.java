package org.wikipedia.styledviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;
import org.wikipedia.WikipediaApp;

public class StylizedEditText extends EditText {
    public StylizedEditText(Context context) {
        super(context);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }

    public StylizedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(((WikipediaApp) context.getApplicationContext()).getPrimaryType());
    }

    public StylizedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(((WikipediaApp) context.getApplicationContext()).getPrimaryType());
    }
}
