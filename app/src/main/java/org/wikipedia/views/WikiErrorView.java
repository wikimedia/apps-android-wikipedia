package org.wikipedia.views;

import org.wikipedia.R;
import org.wikipedia.util.ThrowableUtil;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WikiErrorView extends FrameLayout {

    private TextView errorTextView;
    private Button retryButton;
    private TextView messageTextView;

    public WikiErrorView(Context context) {
        this(context, null);
    }

    public WikiErrorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WikiErrorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(context, R.layout.custom_error_view, this);

        errorTextView = (TextView) findViewById(R.id.error_text);
        retryButton = (Button) findViewById(R.id.retry_button);
        messageTextView = (TextView) findViewById(R.id.server_message_text);
    }

    public void setRetryButtonVisible(boolean visible) {
        retryButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setRetryClickListener(OnClickListener listener) {
        retryButton.setOnClickListener(listener);
    }

    public void setError(@NonNull Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(getContext(), e);
        errorTextView.setText(error.getError());
        messageTextView.setText(error.getDetail());
    }
}