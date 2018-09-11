package org.wikipedia.views;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

public class CircularProgressBar extends View {

    private static final int PROGRESS_START_ANGLE = 270;
    private static final int PROGRESS_BACKGROUND_MIN_ANGLE = 0;
    private static final int PROGRESS_BACKGROUND_MAX_ANGLE = 360;
    private static final int DEFAULT_STROKE_WIDTH_DP = 0;
    private static final int DEFAULT_ANIMATION_DURATION = 1_000;
    private static final double DEFAULT_MAX_PROGRESS = 100.0;
    public static final int MIN_PROGRESS = 5;
    public static final int MAX_PROGRESS = 100;
    private static final String PROPERTY_ANGLE = "angle";

    private Paint progressPaint;
    private Paint progressBackgroundPaint;
    private int sweepAngle = 0;
    private RectF circleBounds;
    private double maxProgressValue = DEFAULT_MAX_PROGRESS;
    private double progressValue;
    private double finalAngle = 0;
    private ValueAnimator progressAnimator;


    public CircularProgressBar(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public CircularProgressBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularProgressBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircularProgressBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {

        int progressColor = ResourceUtil.getThemedColor(getContext(), R.attr.colorAccent);
        int progressBackgroundColor = ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color);
        int progressStrokeWidth = (int) DimenUtil.dpToPx(DEFAULT_STROKE_WIDTH_DP);


        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressBar);
            progressColor = a.getColor(R.styleable.CircularProgressBar_progressColor, progressColor);
            progressBackgroundColor = a.getColor(R.styleable.CircularProgressBar_progressBackgroundColor, progressBackgroundColor);
            progressStrokeWidth = a.getDimensionPixelSize(R.styleable.CircularProgressBar_progressStrokeWidth, progressStrokeWidth);

            a.recycle();
        }

        progressPaint = new Paint();
        progressPaint.setStrokeWidth(progressStrokeWidth);
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);

        progressBackgroundPaint = new Paint();
        progressBackgroundPaint.setStrokeWidth(progressStrokeWidth);
        progressBackgroundPaint.setStyle(Paint.Style.FILL);
        progressBackgroundPaint.setColor(progressBackgroundColor);
        progressBackgroundPaint.setAntiAlias(true);

        circleBounds = new RectF();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        float strokeSizeOffset = progressPaint.getStrokeWidth();
        int desiredSize = (((int) strokeSizeOffset) + Math.max(paddingBottom + paddingTop, paddingLeft + paddingRight));

        desiredSize += desiredSize * .1f;

        int finalWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                finalWidth = measuredWidth;
                break;
            case MeasureSpec.AT_MOST:
                finalWidth = Math.min(desiredSize, measuredWidth);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                finalWidth = desiredSize;
                break;
        }

        int finalHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                finalHeight = measuredHeight;
                break;
            case MeasureSpec.AT_MOST:
                finalHeight = Math.min(desiredSize, measuredHeight);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                finalHeight = desiredSize;
                break;
        }

        int widthWithoutPadding = finalWidth - paddingLeft - paddingRight;
        int heightWithoutPadding = finalHeight - paddingTop - paddingBottom;

        int smallestSide = Math.min(heightWithoutPadding, widthWithoutPadding);
        setMeasuredDimension(smallestSide, smallestSide);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        calculateBounds(w, h);
    }

    private void calculateBounds(int w, int h) {
        float strokeSizeOffset = progressPaint.getStrokeWidth();
        float halfOffset = strokeSizeOffset / 2f;

        circleBounds.left = halfOffset;
        circleBounds.top = halfOffset;
        circleBounds.right = w - halfOffset;
        circleBounds.bottom = h - halfOffset;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawProgressBackground(canvas);
        drawProgress(canvas);
    }

    private void drawProgressBackground(Canvas canvas) {
        canvas.drawArc(circleBounds, PROGRESS_BACKGROUND_MIN_ANGLE, PROGRESS_BACKGROUND_MAX_ANGLE,
                false, progressBackgroundPaint);
    }

    private void drawProgress(Canvas canvas) {
        canvas.drawArc(circleBounds, PROGRESS_START_ANGLE, sweepAngle, true, progressPaint);
    }


    public void setMaxProgress(double maxProgress) {
        maxProgressValue = maxProgress;
        if (maxProgressValue < progressValue) {
            setCurrentProgress(maxProgress);
        }
        invalidate();
    }

    public void setCurrentProgress(double currentProgress) {
        if (currentProgress > maxProgressValue) {
            maxProgressValue = currentProgress;
        }

        setProgress(currentProgress, maxProgressValue);
    }

    public void setProgress(double current, double max) {

        finalAngle = current / max * PROGRESS_BACKGROUND_MAX_ANGLE;

        final PropertyValuesHolder angleProperty = PropertyValuesHolder.ofInt(PROPERTY_ANGLE, sweepAngle, (int) finalAngle);

        double oldCurrentProgress = progressValue;

        maxProgressValue = max;
        progressValue = Math.min(current, max);

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }

        progressAnimator = ValueAnimator.ofObject((TypeEvaluator<Double>) (fraction, startValue, endValue) -> (startValue + (endValue - startValue) * fraction), oldCurrentProgress, progressValue);
        progressAnimator.setDuration(DEFAULT_ANIMATION_DURATION);
        progressAnimator.setValues(angleProperty);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            sweepAngle = (int) animation.getAnimatedValue(PROPERTY_ANGLE);
            invalidate();
        });
        progressAnimator.addListener(new DefaultAnimatorListener());
        progressAnimator.start();
    }

    public void setProgressColor(@ColorInt int color) {
        progressPaint.setColor(color);
        invalidate();
    }

    public void setProgressBackgroundColor(@ColorInt int color) {
        progressBackgroundPaint.setColor(color);
        invalidate();
    }

    @ColorInt
    public int getProgressColor() {
        return progressPaint.getColor();
    }

    @ColorInt
    public int getProgressBackgroundColor() {
        return progressBackgroundPaint.getColor();
    }

    public float getProgressStrokeWidth() {
        return progressPaint.getStrokeWidth();
    }

    class DefaultAnimatorListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(Animator animation, boolean isReverse) {

        }

        @Override
        public void onAnimationEnd(Animator animation, boolean isReverse) {

        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {
            sweepAngle = (int) finalAngle;
            progressAnimator = null;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

}
