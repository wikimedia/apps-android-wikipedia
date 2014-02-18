package org.wikipedia.styledviews;

import android.content.*;
import android.util.*;
import android.widget.*;
import org.wikipedia.*;

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
