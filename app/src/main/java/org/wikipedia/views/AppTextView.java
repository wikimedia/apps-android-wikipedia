package org.wikipedia.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

// TODO: Document where it is desirable to use this class vs. a vanilla TextView
public class AppTextView extends ConfigurableTextView {

    public AppTextView(Context context) {
        super(context);
        init(context, null);
    }

    public AppTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AppTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AppTextView);
            String htmlText = array.getString(R.styleable.AppTextView_html);

            if (htmlText != null) {
                setText(StringUtil.fromHtml(htmlText));
            }

            array.recycle();
        }
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
