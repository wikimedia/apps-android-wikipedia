package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.util.StringUtil;

public class GoneIfEmptyTextView extends TextView {
    public GoneIfEmptyTextView(Context context) {
        super(context);
    }

    public GoneIfEmptyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoneIfEmptyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GoneIfEmptyTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setText(String text) {
        super.setText(text);
        setVisibility(StringUtil.emptyIfNull(text).equals("") ? View.GONE : View.VISIBLE);
    }
}
