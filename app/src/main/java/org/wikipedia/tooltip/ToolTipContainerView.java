package org.wikipedia.tooltip;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.appenguin.onboarding.ToolTipRelativeLayout;
import com.appenguin.onboarding.ToolTipView;

import org.wikipedia.util.ApiUtil;
import org.wikipedia.views.ViewUtil;

import static org.wikipedia.util.DeviceUtil.isBackKeyUp;


/** A one use {@link ToolTipRelativeLayout} that is detached (dismissed) by tapping. For automatic
 * back button support, clients must call {@link #requestFocus}. This {@link android.view.ViewGroup}
 * should only be used to wrap one {@link ToolTipView}. */
@SuppressLint("ViewConstructor")
public class ToolTipContainerView extends ToolTipRelativeLayout {
    private final ToolTipView toolTipView;

    public ToolTipContainerView(ToolTipView toolTipView) {
        super(toolTipView.getContext());

        disableLayoutMirroring();

        this.toolTipView = toolTipView;
        addView(toolTipView);

        // Enable focus so that the client may request focus.
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    // Note: This method will be called even when the view does not have focus.
    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (super.dispatchTouchEvent(event)) {
            // The user clicked the toolTipView itself. The intent was dismissal of the tip, not a
            // click through to something behind the toolTipView. Let the toolTipView handle it.
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Detach on down event instead of up. If we don't handle (return true) on down event,
            // we won't receive the up event. However, if we handle the down event (return true),
            // other views will not receive it or the up event, effectively stealing the click.
            postDetach();
        }

        // User clicked outside the toolTipView. The intent was to click something behind this view.
        // Report as unhandled to allow any views behind it to receive the event.
        return false;
    }

    // Note: This method won't be called when the view doesn't have focus.
    @Override
    public boolean dispatchKeyEventPreIme(@NonNull KeyEvent event) {
        if (isBackKeyUp(event)) {
            postDetach();
            return true;
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public void removeView(@NonNull View view) {
        super.removeView(view);

        if (view == toolTipView) {
            detach();
        }
    }

    /** Removes the view and child, a {@link ToolTipView}, possibly after a brief animation. */
    public void postDetach() {
        toolTipView.remove();
    }

    private void detach() {
        ViewUtil.detach(this);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void disableLayoutMirroring() {
        if (ApiUtil.hasJellyBeanMr1()) {
            // Onboarding does not handle mirroring when calculating layout offsets.
            setLayoutDirection(LAYOUT_DIRECTION_LTR);
        }
    }
}
