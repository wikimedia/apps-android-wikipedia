package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.TextView;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class ConfigurableTextView extends TextView {
    public ConfigurableTextView(Context context) {
        super(context);
    }

    public ConfigurableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfigurableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConfigurableTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setText(CharSequence text, String languageCode) {
        super.setText(text);
        setLocale(languageCode);
    }

    public void setText(Spanned text, String languageCode) {
        super.setText(text);
        setLocale(languageCode);
    }

    public void setLocale(String languageCode) {
        setConditionalLayoutDirection(this, languageCode);
    }
}
