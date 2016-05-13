package org.wikipedia.views;

import org.wikipedia.R;
import org.wikipedia.util.ThrowableUtil;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.ThrowableUtil.isRetryable;

public class WikiErrorView extends FrameLayout {
    @BindView(R.id.error_text) TextView errorTextView;
    @BindView(R.id.retry_button) Button button;
    @BindView(R.id.server_message_text) TextView messageTextView;

    @Nullable private OnClickListener retryListener;
    @Nullable private OnClickListener backListener;

    public WikiErrorView(Context context) {
        this(context, null);
    }

    public WikiErrorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WikiErrorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(context, R.layout.custom_error_view, this);
        ButterKnife.bind(this);
    }

    public void setRetryClickListener(@Nullable OnClickListener listener) {
        retryListener = listener;
    }

    public void setBackClickListener(@Nullable OnClickListener listener) {
        backListener = listener;
    }

    public void setError(@NonNull Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(getContext(), e);
        errorTextView.setText(error.getError());
        messageTextView.setText(error.getDetail());
        updateButton(isRetryable(error));
    }

    private void updateButton(boolean retryable) {
        OnClickListener listener = retryable ? retryListener : backListener;
        button.setVisibility(listener == null ? GONE : VISIBLE);
        button.setText(retryable ? R.string.page_error_retry : R.string.page_error_back_to_main);
        button.setOnClickListener(listener);
    }
}