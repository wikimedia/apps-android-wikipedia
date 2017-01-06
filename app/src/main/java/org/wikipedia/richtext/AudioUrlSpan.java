package org.wikipedia.richtext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.drawable.AppLevelListDrawable;
import org.wikipedia.drawable.CircularProgressDrawable;
import org.wikipedia.media.AvPlayer;

public class AudioUrlSpan extends AnimatedImageSpan {

    private static final int STOP_ICON_LEVEL = 0;
    private static final int PLAY_ICON_LEVEL = 1;

    @NonNull
    private final AvPlayer player;

    @NonNull
    private final AvCallback avCallback = new AvCallback();

    @NonNull
    private final String path;

    public AudioUrlSpan(@NonNull View view,
                        @NonNull AvPlayer player,
                        @NonNull String path,
                        int verticalAlignment) {
        super(view, drawable(view.getContext()), verticalAlignment);
        this.player = player;
        this.path = path;
    }

    public void setTint(@ColorInt int color) {
        DrawableCompat.setTint(getDrawable(), color);
    }

    @Override
    public void start() {
        showIcon(PLAY_ICON_LEVEL);
        super.start();
        player.play(path, avCallback, avCallback);
    }

    @Override
    public void stop() {
        showIcon(STOP_ICON_LEVEL);
        super.stop();
        player.stop();
    }

    @Override
    public boolean isRunning() {
        return getIconShown() == PLAY_ICON_LEVEL;
    }

    @NonNull
    @Override
    public Drawable getDrawable() {
        //noinspection ConstantConditions
        return super.getDrawable();
    }

    private void showIcon(int level) {
        getDrawable().setLevel(level);
    }

    private int getIconShown() {
        return getDrawable().getLevel();
    }

    @NonNull
    private static Drawable drawable(Context context) {
        LevelListDrawable levels = new AppLevelListDrawable();
        levels.addLevel(PLAY_ICON_LEVEL, PLAY_ICON_LEVEL, spinnerDrawable(context));
        levels.addLevel(STOP_ICON_LEVEL, STOP_ICON_LEVEL, speakerDrawable(context));
        int radius = getDimensionPixelSize(context, R.dimen.audio_url_span_loading_spinner_radius);
        levels.setBounds(0, 0, radius * 2, radius * 2);
        return levels;
    }

    @NonNull
    private static Drawable speakerDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_volume_up_black_24dp);
    }

    @NonNull
    private static Drawable spinnerDrawable(Context context) {
        return new CircularProgressDrawable(Color.WHITE,
                getDimensionPixelSize(context, R.dimen.audio_url_span_loading_spinner_border_thickness),
                getDimensionPixelSize(context, R.dimen.audio_url_span_loading_spinner_radius));
    }

    private static int getDimensionPixelSize(Context context, @DimenRes int id) {
        return context.getResources().getDimensionPixelSize(id);
    }

    private class AvCallback implements AvPlayer.Callback, AvPlayer.ErrorCallback {
        @Override
        public void onSuccess() {
            stop();
        }


        @Override
        public void onError() {
            stop();
        }
    }
}
