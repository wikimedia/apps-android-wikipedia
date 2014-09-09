package org.wikipedia.beta.views;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.events.WebViewInvalidateEvent;
import android.graphics.Canvas;

public class ObservableWebView extends WebView {
    private OnScrollChangeListener onScrollChangeListener;
    private OnDownMotionEventListener onDownMotionEventListener;
    private OnUpOrCancelMotionEventListener onUpOrCancelMotionEventListener;
    private OnFrustratedScrollListener onFrustratedScrollListener;

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

    public OnFrustratedScrollListener getOnFrustratedScrollListener() {
        return onFrustratedScrollListener;
    }

    public void setOnFrustratedScrollListener(OnFrustratedScrollListener onFrustratedScrollListener) {
        this.onFrustratedScrollListener = onFrustratedScrollListener;
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

    public interface OnFrustratedScrollListener {
        void onFrustratedScroll();
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


    private long lastScrollTime;
    private int totalAmountScrolled;

    /**
     * Threshold (in dp) of continuous scrolling, to be considered "frustrated" scrolling.
     */
    private static final int SCROLL_FRUSTRATION_THRESHOLD = 3000;

    /**
     * Maximum single scroll amount to be considered a "human" scroll.
     * Otherwise it's probably a programmatic scroll, which we won't count.
     */
    private static final int MAX_HUMAN_SCROLL = 500;

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChanged(oldt, t);
        }
        //make sure it's a human scroll
        if (Math.abs(t - oldt) > (int)(MAX_HUMAN_SCROLL * getResources().getDisplayMetrics().density)) {
            return;
        }
        totalAmountScrolled += (t - oldt);
        if (Math.abs(totalAmountScrolled) > (int)(SCROLL_FRUSTRATION_THRESHOLD * getResources().getDisplayMetrics().density)
            && onFrustratedScrollListener != null) {
            onFrustratedScrollListener.onFrustratedScroll();
            totalAmountScrolled = 0;
        }
        lastScrollTime = System.currentTimeMillis();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (onDownMotionEventListener != null && onUpOrCancelMotionEventListener != null) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    onDownMotionEventListener.onDownMotionEvent();
                    if (System.currentTimeMillis() - lastScrollTime > DateUtils.SECOND_IN_MILLIS / 2) {
                        totalAmountScrolled = 0;
                    }
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        WikipediaApp.getInstance().getBus().post(new WebViewInvalidateEvent());
    }
}
