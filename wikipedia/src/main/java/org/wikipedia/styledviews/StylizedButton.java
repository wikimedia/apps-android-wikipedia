package org.wikipedia.styledviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;
import org.wikipedia.WikipediaApp;

public class StylizedButton extends Button {
    public StylizedButton(Context context) {
        super(context);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }

    public StylizedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(((WikipediaApp) context.getApplicationContext()).getPrimaryType());
    }

    public StylizedButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(((WikipediaApp) context.getApplicationContext()).getPrimaryType());
    }
}
