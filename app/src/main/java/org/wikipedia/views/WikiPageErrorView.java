package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import static org.wikipedia.util.ThrowableUtil.isOffline;

public class WikiPageErrorView extends WikiErrorView {

    public WikiPageErrorView(Context context) {
        super(context);
    }

    public WikiPageErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WikiPageErrorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override ErrorType getErrorType(@Nullable Throwable caught) {
        if (caught != null && isOffline(caught)) {
            return ErrorType.PAGE_OFFLINE;
        }
        return super.getErrorType(caught);
    }
}
