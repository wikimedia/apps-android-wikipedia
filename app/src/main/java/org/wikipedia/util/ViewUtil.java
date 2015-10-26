package org.wikipedia.util;

import android.view.ActionMode;
import android.view.View;

import java.lang.reflect.Field;

public final class ViewUtil {

    /**
     * Find the originating view of an ActionMode.
     * @param mode The ActionMode in question.
     * @return The view from which the ActionMode originated.
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static View getOriginatingView(ActionMode mode) throws NoSuchFieldException, IllegalAccessException {
        Field originatingView = mode.getClass().getDeclaredField("mOriginatingView");
        originatingView.setAccessible(true);
        return (View) originatingView.get(mode);
    }

    private ViewUtil() {

    }
}
