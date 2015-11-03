package org.wikipedia.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.Property;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

// https://gist.github.com/castorflex/4e46a9dc2c3a4245a28e
public class CircularProgressDrawable extends AnimationDrawable {
    @NonNull private static final Interpolator ANGLE_INTERPOLATOR = new LinearInterpolator();
    @NonNull private static final Interpolator SWEEP_INTERPOLATOR = new DecelerateInterpolator();
    private static final int ANGLE_ANIMATOR_DURATION = 2000;
    private static final int SWEEP_ANIMATOR_DURATION = 600;
    private static final int MIN_SWEEP_ANGLE = 30;
    private static final int MAX_SWEEP_ANGLE = 360;
    @NonNull private final RectF fBounds = new RectF();

    private ObjectAnimator objectAnimatorSweep;
    private ObjectAnimator objectAnimatorAngle;
    private boolean modeAppearing;
    private Paint paint;
    private float currentGlobalAngleOffset;
    private float currentGlobalAngle;
    private float currentSweepAngle;
    private float borderWidth;

    public CircularProgressDrawable(@ColorInt int color, float borderWidth, int radius) {
        this(color, borderWidth);
        setBounds(0, 0, radius * 2, radius * 2);
        start();
    }

    public CircularProgressDrawable(@ColorInt int color, float borderWidth) {
        this.borderWidth = borderWidth;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        paint.setColor(color);

        setupAnimations();
    }

    @Override
    public void draw(Canvas canvas) {
        float startAngle = currentGlobalAngle - currentGlobalAngleOffset;
        float sweepAngle = currentSweepAngle;
        if (!modeAppearing) {
            startAngle = startAngle + sweepAngle;
            sweepAngle = MAX_SWEEP_ANGLE - sweepAngle - MIN_SWEEP_ANGLE;
        } else {
            sweepAngle += MIN_SWEEP_ANGLE;
        }
        canvas.drawArc(fBounds, startAngle, sweepAngle, false, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        super.setTintList(tint);
        @ColorInt int color = tint.getColorForState(getState(), tint.getDefaultColor());
        setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void start() {
        super.start();
        if (isRunning()) {
            return;
        }
        objectAnimatorAngle.start();
        objectAnimatorSweep.start();
        invalidateSelf();
    }

    @Override
    public void stop() {
        super.stop();
        if (!isRunning()) {
            return;
        }
        objectAnimatorAngle.cancel();
        objectAnimatorSweep.cancel();
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        fBounds.left = bounds.left + borderWidth / 2f + 1 / 2f;
        fBounds.right = bounds.right - borderWidth / 2f - 1 / 2f;
        fBounds.top = bounds.top + borderWidth / 2f + 1 / 2f;
        fBounds.bottom = bounds.bottom - borderWidth / 2f - 1 / 2f;
    }

    public void setCurrentGlobalAngle(float currentGlobalAngle) {
        this.currentGlobalAngle = currentGlobalAngle;
        invalidateSelf();
    }

    public float getCurrentGlobalAngle() {
        return currentGlobalAngle;
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        this.currentSweepAngle = currentSweepAngle;
        invalidateSelf();
    }

    public float getCurrentSweepAngle() {
        return currentSweepAngle;
    }

    private void toggleAppearingMode() {
        modeAppearing = !modeAppearing;
        if (modeAppearing) {
            currentGlobalAngleOffset = (currentGlobalAngleOffset + MIN_SWEEP_ANGLE * 2) % MAX_SWEEP_ANGLE;
        }
    }

    private Property<CircularProgressDrawable, Float> mAngleProperty
            = new Property<CircularProgressDrawable, Float>(Float.class, "angle") {
        @Override
        public Float get(CircularProgressDrawable object) {
            return object.getCurrentGlobalAngle();
        }

        @Override
        public void set(CircularProgressDrawable object, Float value) {
            object.setCurrentGlobalAngle(value);
        }
    };

    private Property<CircularProgressDrawable, Float> mSweepProperty
            = new Property<CircularProgressDrawable, Float>(Float.class, "arc") {
        @Override
        public Float get(CircularProgressDrawable object) {
            return object.getCurrentSweepAngle();
        }

        @Override
        public void set(CircularProgressDrawable object, Float value) {
            object.setCurrentSweepAngle(value);
        }
    };

    private void setupAnimations() {
        objectAnimatorAngle = ObjectAnimator.ofFloat(this, mAngleProperty, MAX_SWEEP_ANGLE);
        objectAnimatorAngle.setInterpolator(ANGLE_INTERPOLATOR);
        objectAnimatorAngle.setDuration(ANGLE_ANIMATOR_DURATION);
        objectAnimatorAngle.setRepeatMode(ValueAnimator.RESTART);
        objectAnimatorAngle.setRepeatCount(ValueAnimator.INFINITE);

        objectAnimatorSweep = ObjectAnimator.ofFloat(this, mSweepProperty, MAX_SWEEP_ANGLE - MIN_SWEEP_ANGLE * 2);
        objectAnimatorSweep.setInterpolator(SWEEP_INTERPOLATOR);
        objectAnimatorSweep.setDuration(SWEEP_ANIMATOR_DURATION);
        objectAnimatorSweep.setRepeatMode(ValueAnimator.RESTART);
        objectAnimatorSweep.setRepeatCount(ValueAnimator.INFINITE);
        objectAnimatorSweep.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                toggleAppearingMode();
            }
        });
    }
}
