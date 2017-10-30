package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import org.wikipedia.util.DimenUtil;

public class LinearLayoutOverWebView extends LinearLayout {
    private ObservableWebView webView;
    private int touchSlop;

    private boolean isPressed = false;
    private int amountScrolled;
    private float startY;
    private boolean slopReached;

    public LinearLayoutOverWebView(Context context) {
        super(context);
    }

    public LinearLayoutOverWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearLayoutOverWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWebView(@NonNull ObservableWebView webView) {
        this.webView = webView;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                slopReached = false;
                startY = event.getY();
                amountScrolled = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isPressed && webView != null) {
                    int contentHeight = (int) (webView.getContentHeight() * DimenUtil.getDensityScalar());
                    int minScroll = -webView.getScrollY();
                    int maxScroll = contentHeight - webView.getScrollY() - webView.getHeight();
                    int scrollAmount = Math.min((int) (startY - event.getY()), maxScroll);
                    scrollAmount = Math.max(minScroll, scrollAmount);
                    // manually scroll the WebView that's underneath us...
                    webView.scrollBy(0, scrollAmount);
                    amountScrolled += scrollAmount;
                    if (Math.abs(amountScrolled) > touchSlop && !slopReached) {
                        // if we go outside the slop radius, then dispatch a Cancel event to
                        // our children, and no longer dispatch any other events until we're
                        // finished with the current gesture.
                        slopReached = true;
                        MotionEvent moveEvent = MotionEvent.obtain(event);
                        moveEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(moveEvent);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                slopReached = false;
                break;
            default:
                break;
        }
        return slopReached || super.dispatchTouchEvent(event);
    }
}
