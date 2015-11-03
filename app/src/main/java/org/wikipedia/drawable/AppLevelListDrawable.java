package org.wikipedia.drawable;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.support.annotation.Nullable;

import org.wikipedia.util.ApiUtil;

/** A {@link LevelListDrawable} that applies color filters and tints to each level. */
public class AppLevelListDrawable extends LevelListDrawable {
    @Nullable private ColorFilter colorFilter;
    @Nullable private ColorStateList tint;

    @Override
    @Nullable
    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        this.colorFilter = colorFilter;
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        super.setTintList(tint);
        this.tint = tint;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean invalidate = super.onLevelChange(level);

        invalidate |= updateLevelTint(getCurrent());

        return invalidate;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean updateLevelTint(Drawable drawable) {
        boolean invalidate = false;
        if (drawable != null) {
            drawable.setColorFilter(colorFilter);
            if (ApiUtil.hasLollipop()) {
                drawable.setTintList(tint);
            }
            invalidate = true;
        }
        return invalidate;
    }
}