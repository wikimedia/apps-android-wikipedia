package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.WebViewInvalidateEvent;
import android.graphics.Canvas;

public class ObservableWebView extends WebView {
    private OnScrollChangeListener onScrollChangeListener;
    private OnDownMotionEventListener onDownMotionEventListener;
    private OnUpOrCancelMotionEventListener onUpOrCancelMotionEventListener;

    public OnScrollChangeListener getOnScrollChangeListener() {
        return onScrollChangeListener;
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public OnDownMotionEventListener getOnDownMotionEventListener() {
        return onDownMotionEventListener;
    }

    public void setOnDownMotionEventListener(OnDownMotionEventListener onDownMotionEventListener) {
        this.onDownMotionEventListener = onDownMotionEventListener;
    }

    public OnUpOrCancelMotionEventListener getOnUpOrCancelMotionEventListener() {
        return onUpOrCancelMotionEventListener;
    }

    public void setOnUpOrCancelMotionEventListener(OnUpOrCancelMotionEventListener onUpOrCancelMotionEventListener) {
        this.onUpOrCancelMotionEventListener = onUpOrCancelMotionEventListener;
    }

    public interface OnScrollChangeListener {
        void onScrollChanged(int oldScrollY, int scrollY);
    }

    public interface  OnDownMotionEventListener {
        void onDownMotionEvent();
    }

    public interface OnUpOrCancelMotionEventListener {
        void onUpOrCancelMotionEvent();
    }

    public ObservableWebView(Context context) {
        super(context);
    }

    public ObservableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObservableWebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChanged(oldt, t);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (onDownMotionEventListener != null && onUpOrCancelMotionEventListener != null) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    onDownMotionEventListener.onDownMotionEvent();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    onUpOrCancelMotionEventListener.onUpOrCancelMotionEvent();
                    break;
                default:
                    // Do nothing for all the other things
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw (Canvas canvas) {
        super.onDraw(canvas);
        WikipediaApp.getInstance().getBus().post(new WebViewInvalidateEvent());
    }
}
