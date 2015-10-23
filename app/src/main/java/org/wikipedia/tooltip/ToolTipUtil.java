package org.wikipedia.tooltip;

import android.app.Activity;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.appenguin.onboarding.ToolTip;
import com.appenguin.onboarding.ToolTipView;

import org.wikipedia.R;
import org.wikipedia.activity.ActivityUtil;

import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;

public final class ToolTipUtil {
    private static final int TOOL_TIP_VIEW_ID = R.id.view_tool_tip_container;

    /**
     * @param activity The Activity whose view hierarchy the tool tip will reside in.
     * @param targetView The view the tool tip should point at.
     * @param contentLayout The layout resource to inflate into the tool tip.
     * @param color The color resource for the tool tip background.
     * @param position The position of the tool tip relative the targetView.
     **/
    public static void showToolTip(Activity activity,
                                   View targetView,
                                   @LayoutRes int contentLayout,
                                   @ColorInt int color,
                                   ToolTip.Position position) {
        removeToolTip(activity);

        ToolTipContainerView toolTip = buildToolTip(targetView, contentLayout, color, position);

        addToolTip(activity, toolTip);
    }

    public static void showToolTip(Activity activity,
                                   View targetView,
                                   @LayoutRes int contentLayout,
                                   ToolTip.Position position) {
        int color = activity.getResources().getColor(getThemedAttributeId(activity, R.attr.tool_tip_default_color));
        showToolTip(activity, targetView, contentLayout, color, position);
    }

    /** @return True if dismissed, false if not present. */
    public static boolean dismissToolTip(Activity activity) {
        return removeToolTip(activity);
    }

    // Injects the tool tip right under the root view on top of everything else.
    private static void addToolTip(Activity activity, ToolTipContainerView toolTip) {
        ((ViewGroup) ActivityUtil.getRootView(activity)).addView(toolTip);
    }

    private static boolean removeToolTip(@NonNull Activity activity) {
        ToolTipContainerView toolTip = findToolTip(ActivityUtil.getRootView(activity));
        if (toolTip != null) {
            toolTip.postDetach();

            // Since detach is asynchronous, avoid potential race conditions by clearing the ID.
            toolTip.setId(View.NO_ID);

            return true;
        }
        return false;
    }

    @Nullable
    private static ToolTipContainerView findToolTip(@NonNull View view) {
        return (ToolTipContainerView) view.findViewById(TOOL_TIP_VIEW_ID);
    }

    /** Assembles a tool tip and its contents. */
    private static ToolTipContainerView buildToolTip(View targetView,
                                                     @LayoutRes int contentLayout,
                                                     @ColorInt int color,
                                                     ToolTip.Position position) {
        ToolTipView contentView = new ToolTipView(targetView.getContext());

        ToolTipContainerView containerView = buildToolTipContainerView(contentView);

        ToolTip content = buildToolTipContent(targetView, contentLayout, color, position);
        contentView.setToolTip(content, targetView);

        return containerView;
    }

    private static ToolTip buildToolTipContent(View targetView,
                                               @LayoutRes int contentLayout,
                                               @ColorInt int color,
                                               ToolTip.Position position) {
        return new ToolTip()
                .withColor(color)
                .withContentView(View.inflate(targetView.getContext(), contentLayout, null))
                .withPosition(position);
    }

    private static ToolTipContainerView buildToolTipContainerView(ToolTipView contentView) {
        ToolTipContainerView containerView = new ToolTipContainerView(contentView);
        containerView.setId(TOOL_TIP_VIEW_ID);
        return containerView;
    }

    private ToolTipUtil() { }
}