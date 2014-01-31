package org.wikipedia.styledviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.TextView;
import org.wikipedia.WikipediaApp;

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
