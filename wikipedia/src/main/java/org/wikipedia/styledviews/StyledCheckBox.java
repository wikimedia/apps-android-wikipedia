package org.wikipedia.styledviews;

import android.content.*;
import android.util.*;
import android.widget.*;
import org.wikipedia.*;

public class StyledCheckBox extends CheckBox {
    public StyledCheckBox(Context context) {
        super(context);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }

    public StyledCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }

    public StyledCheckBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(((WikipediaApp)context.getApplicationContext()).getPrimaryType());
    }
}
