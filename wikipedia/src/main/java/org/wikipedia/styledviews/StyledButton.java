package org.wikipedia.styledviews;

import android.content.*;
import android.util.*;
import android.widget.*;
import org.wikipedia.*;

public class StyledButton extends Button {
    public StyledButton(Context context) {
        this(context, null);
    }

    public StyledButton(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.buttonStyle);
    }

    public StyledButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode()) {
            setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
        }
    }
}
