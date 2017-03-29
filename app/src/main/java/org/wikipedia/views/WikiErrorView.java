package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.ThrowableUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.ThrowableUtil.is404;

public class WikiErrorView extends FrameLayout {
    @BindView(R.id.error_text) TextView errorTextView;
    @BindView(R.id.retry_button) TextView button;
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
        inflate(context, R.layout.view_wiki_error, this);
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
        updateButton(is404(error));
    }

    private void updateButton(boolean is404) {
        OnClickListener listener = is404 ? backListener : retryListener;
        button.setVisibility(listener == null ? GONE : VISIBLE);
        button.setText(is404 ? R.string.page_error_back_to_main : R.string.page_error_retry);
        button.setOnClickListener(listener);
    }
}
