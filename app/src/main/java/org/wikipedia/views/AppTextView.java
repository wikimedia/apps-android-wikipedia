package org.wikipedia.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

// TODO: Document where it is desirable to use this class vs. a vanilla TextView
public class AppTextView extends ConfigurableTextView {

    public AppTextView(Context context) {
        super(context);
    }

    public AppTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Workaround for https://code.google.com/p/android/issues/detail?id=191430
        // which only occurs on API 23
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    && getSelectionStart() != getSelectionEnd()) {
                CharSequence text = getText();
                setText(null);
                setText(text);
            }
        }
        try {
            // Workaround for some obscure AOSP crashes when highlighting text.
            return super.dispatchTouchEvent(event);
        } catch (Exception e) {
            // ignore
        }
        return true;
    }
}
