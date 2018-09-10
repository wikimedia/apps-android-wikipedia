package org.wikipedia.util;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.NonNull;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.DraweeTransition;

public final class AnimationUtil {

    public static void setSharedElementTransitions(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // For using shared element transitions with Fresco, we need to explicitly define
            // a DraweeTransition that will be automatically used by Drawees that are used in
            // transitions between activities.
            activity.getWindow().setSharedElementEnterTransition(DraweeTransition
                    .createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_CROP));
            activity.getWindow().setSharedElementReturnTransition(DraweeTransition
                    .createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_CROP));
        }
    }

    private AnimationUtil() { }
}
