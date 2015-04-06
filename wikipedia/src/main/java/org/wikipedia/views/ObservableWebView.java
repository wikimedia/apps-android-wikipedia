package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.WebViewInvalidateEvent;
import android.graphics.Canvas;
import java.util.ArrayList;
import java.util.List;

public class ObservableWebView extends WebView {
    private static final WebViewInvalidateEvent INVALIDATE_EVENT = new WebViewInvalidateEvent();

    private List<OnClickListener> onClickListeners;
    private List<OnLongPressListener> onLongPressListeners;
    private List<OnScrollChangeListener> onScrollChangeListeners;
    private List<OnDownMotionEventListener> onDownMotionEventListeners;
    private List<OnUpOrCancelMotionEventListener> onUpOrCancelMotionEventListeners;
    private List<OnContentHeightChangedListener> onContentHeightChangedListeners;

    private int contentHeight = 0;
    private float touchStartX;
    private float touchStartY;
    private int touchSlop;

    public void addOnClickListener(OnClickListener onClickListener) {
        onClickListeners.add(onClickListener);
    }

    public void addOnLongPressListener(OnLongPressListener onLongPressListener) {
        onLongPressListeners.add(onLongPressListener);
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

    public interface OnClickListener {
        boolean onClick(float x, float y);
    }

    public interface OnLongPressListener {
        boolean onLongPress(float x, float y, String linkTitle);
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

    public interface OnContentHeightChangedListener {
        void onContentHeightChanged(int contentHeight);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ObservableWebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        init();
    }

    private void init() {
        onClickListeners = new ArrayList<>();
        onLongPressListeners = new ArrayList<>();
        onScrollChangeListeners = new ArrayList<>();
        onDownMotionEventListeners = new ArrayList<>();
        onUpOrCancelMotionEventListeners = new ArrayList<>();
        onContentHeightChangedListeners = new ArrayList<>();
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        for (OnScrollChangeListener listener : onScrollChangeListeners) {
            listener.onScrollChanged(oldt, t);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                for (OnDownMotionEventListener listener : onDownMotionEventListeners) {
                    listener.onDownMotionEvent();
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

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        HitTestResult result = getHitTestResult();
        if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
            Uri uri = Uri.parse(result.getExtra());
            final String authority = uri.getAuthority();
            if ("wikipedia.org".equals(authority)) {
                for (OnLongPressListener listener : onLongPressListeners) {
                    if (listener.onLongPress(touchStartX, touchStartY, uri.getPath())) {
                        break;
                    }
                }
            }
        }
    }
}
