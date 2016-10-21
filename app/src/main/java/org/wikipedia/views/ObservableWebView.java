package org.wikipedia.views;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebView;

import org.wikipedia.WikipediaApp;
import org.wikipedia.events.WebViewInvalidateEvent;
import org.wikipedia.util.DimenUtil;

import java.util.ArrayList;
import java.util.List;

public class ObservableWebView extends WebView {
    private static final WebViewInvalidateEvent INVALIDATE_EVENT = new WebViewInvalidateEvent();

    private List<OnClickListener> onClickListeners;
    private List<OnScrollChangeListener> onScrollChangeListeners;
    private List<OnDownMotionEventListener> onDownMotionEventListeners;
    private List<OnUpOrCancelMotionEventListener> onUpOrCancelMotionEventListeners;
    private List<OnContentHeightChangedListener> onContentHeightChangedListeners;
    private OnFastScrollListener onFastScrollListener;

    private int contentHeight = 0;
    private float touchStartX;
    private float touchStartY;
    private int touchSlop;

    private long lastScrollTime;
    private int totalAmountScrolled;

    /**
    * Threshold (in pixels) of continuous scrolling, to be considered "fast" scrolling.
    */
    private static final int FAST_SCROLL_THRESHOLD = (int) (1000 * DimenUtil.getDensityScalar());

    /**
    * Maximum single scroll amount (in pixels) to be considered a "human" scroll.
    * Otherwise it's probably a programmatic scroll, which we won't count.
    */
    private static final int MAX_HUMAN_SCROLL = (int) (500 * DimenUtil.getDensityScalar());

    /**
     * Maximum amount of time that needs to elapse before the previous scroll amount
     * is "forgotten." That is, if the user scrolls once, then scrolls again within this
     * time, then the two scroll actions will be added together as one, and counted towards
     * a possible "fast" scroll.
     */
    private static final int MAX_MILLIS_BETWEEN_SCROLLS = 500;

    public void addOnClickListener(OnClickListener onClickListener) {
        onClickListeners.add(onClickListener);
    }

    public void addOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        onScrollChangeListeners.add(onScrollChangeListener);
    }

    public void addOnDownMotionEventListener(OnDownMotionEventListener onDownMotionEventListener) {
        onDownMotionEventListeners.add(onDownMotionEventListener);
    }

    public void addOnUpOrCancelMotionEventListener(OnUpOrCancelMotionEventListener onUpOrCancelMotionEventListener) {
        onUpOrCancelMotionEventListeners.add(onUpOrCancelMotionEventListener);
    }

    public void addOnContentHeightChangedListener(OnContentHeightChangedListener onContentHeightChangedListener) {
        onContentHeightChangedListeners.add(onContentHeightChangedListener);
    }

    public void setOnFastScrollListener(OnFastScrollListener onFastScrollListener) {
        this.onFastScrollListener = onFastScrollListener;
    }

    public void clearAllListeners() {
        onClickListeners.clear();
        onScrollChangeListeners.clear();
        onDownMotionEventListeners.clear();
        onUpOrCancelMotionEventListeners.clear();
        onContentHeightChangedListeners.clear();
        onFastScrollListener = null;
    }

    public interface OnClickListener {
        boolean onClick(float x, float y);
    }

    public interface OnScrollChangeListener {
        void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll);
    }

    public interface OnDownMotionEventListener {
        void onDownMotionEvent();
    }

    public interface OnUpOrCancelMotionEventListener {
        void onUpOrCancelMotionEvent();
    }

    public interface OnContentHeightChangedListener {
        void onContentHeightChanged(int contentHeight);
    }

    public interface OnFastScrollListener {
        void onFastScroll();
    }

    public void copyToClipboard() {
        // Simulate a Ctrl-C key press, which copies the current selection to the clipboard.
        // Seems to work across all APIs.
        dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON));
        dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON));
    }

    public ObservableWebView(Context context) {
        super(context);
        init();
    }

    public ObservableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ObservableWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        onClickListeners = new ArrayList<>();
        onScrollChangeListeners = new ArrayList<>();
        onDownMotionEventListeners = new ArrayList<>();
        onUpOrCancelMotionEventListeners = new ArrayList<>();
        onContentHeightChangedListeners = new ArrayList<>();
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        super.onScrollChanged(left, top, oldLeft, oldTop);
        boolean isHumanScroll = Math.abs(top - oldTop) < MAX_HUMAN_SCROLL;
        for (OnScrollChangeListener listener : onScrollChangeListeners) {
            listener.onScrollChanged(oldTop, top, isHumanScroll);
        }
        if (!isHumanScroll) {
            return;
        }
        totalAmountScrolled += (top - oldTop);
        if (Math.abs(totalAmountScrolled) > FAST_SCROLL_THRESHOLD
                && onFastScrollListener != null) {
            onFastScrollListener.onFastScroll();
            totalAmountScrolled = 0;
        }
        lastScrollTime = System.currentTimeMillis();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                for (OnDownMotionEventListener listener : onDownMotionEventListeners) {
                    listener.onDownMotionEvent();
                }
                if (System.currentTimeMillis() - lastScrollTime > MAX_MILLIS_BETWEEN_SCROLLS) {
                    totalAmountScrolled = 0;
                }
                touchStartX = event.getX();
                touchStartY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - touchStartX) <= touchSlop
                        && Math.abs(event.getY() - touchStartY) <= touchSlop) {
                    for (OnClickListener listener : onClickListeners) {
                        if (listener.onClick(event.getX(), event.getY())) {
                            return true;
                        }
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                for (OnUpOrCancelMotionEventListener listener : onUpOrCancelMotionEventListeners) {
                    listener.onUpOrCancelMotionEvent();
                }
                break;
            default:
                // Do nothing for all the other things
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode()) {
            return;
        }
        if (contentHeight != getContentHeight()) {
            contentHeight = getContentHeight();
            for (OnContentHeightChangedListener listener : onContentHeightChangedListeners) {
                listener.onContentHeightChanged(contentHeight);
            }
        }
        WikipediaApp.getInstance().getBus().post(INVALIDATE_EVENT);
    }
}
