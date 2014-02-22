package org.wikipedia.styledviews;

import android.content.*;
import android.util.*;
import android.widget.*;
import org.wikipedia.*;

public class StylizedButton extends Button {
    public StylizedButton(Context context) {
        super(context);
        if (!isInEditMode()) {
            setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
        }
    }

    public StylizedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
        }
    }

    public StylizedButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode()) {
            setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
        }
    }
}
