package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class GoneIfEmptyTextView extends AppTextView {
    public GoneIfEmptyTextView(Context context) {
        super(context);
    }

    public GoneIfEmptyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoneIfEmptyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }
}
