package org.wikipedia.beta.nearby;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.widget.ImageView;

public class NearbyCompassView extends ImageView {
    private static final String TAG = "NearbyCompassView";

    private Paint paintTick;
    private Paint paintArrow;
    private int tickColor = Color.BLACK;
    private int maskColor = Color.WHITE;

    // make the mask bitmap static, so that it's re-used by all instances of this view
    // (we're assuming that all instances will be the same size)
    private static Bitmap MASK_BMP;
    private static Paint MASK_BMP_PAINT;

    private float displayDensity;
    private static final float TICK_WIDTH = 1.0f;
    private static final int TICK_LENGTH = 3;
    private static final int TICK_OFFSET = 8;
    private static final int NUM_TICKS = 60;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 11;
    private static final int ARROW_FRUSTUM = 4;
    private Path arrowPath;

    private float baseAngle = 0f;
    private float azimuth = 0f;

    public NearbyCompassView(Context context) {
        super(context);
        init();
    }

    public NearbyCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NearbyCompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        displayDensity = getResources().getDisplayMetrics().density;

        paintTick = new Paint();
        paintTick.setAntiAlias(true);
        paintTick.setColor(tickColor);
        paintTick.setStyle(Paint.Style.STROKE);
        paintTick.setStrokeWidth(TICK_WIDTH * displayDensity);

        paintArrow = new Paint();
        paintArrow.setAntiAlias(true);
        paintArrow.setColor(tickColor);
        paintArrow.setStyle(Paint.Style.FILL);
    }

    /**
     * Set the "base" angle offset from North (in degrees), moving counterclockwise.
     * For example, an angle of 90 will make the arrow point due West.
     * @param angle Angle offset.
     */
    public void setAngle(float angle) {
        this.baseAngle = angle;
        invalidate();
    }

    /**
     * Set the azimuth, which will be added to the base angle offset for our arrow.
     * For example, if the base angle is 90, and the azimuth is 45, then the arrow
     * will point Southwest.
     * @param azimuth Azimuth to be added to the base angle.
     */
    public void setAzimuth(float azimuth) {
        // if it's an insignificant change, then don't worry about it
        if (Math.abs(azimuth - this.azimuth) < 1.0f) {
            return;
        }
        this.azimuth = azimuth;
        invalidate();
    }

    public void setTickColor(int color) {
        this.tickColor = color;
        init();
        invalidate();
    }

    public void setMaskColor(int color) {
        if (this.maskColor != color && MASK_BMP != null) {
            MASK_BMP.recycle();
            MASK_BMP = null;
        }
        this.maskColor = color;
        init();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //draw the original image...
        super.onDraw(canvas);

        float w = this.getWidth();
        float h = this.getHeight();
        float centerX = w / 2;
        float centerY = h / 2;

        //draw the circular mask bitmap
        if (MASK_BMP == null) {
            MASK_BMP = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas bmpCanvas = new Canvas(MASK_BMP);
            bmpCanvas.drawColor(maskColor);
            Paint maskPaint = new Paint();
            maskPaint.setStyle(Paint.Style.FILL);
            maskPaint.setColor(Color.TRANSPARENT);
            maskPaint.setAntiAlias(true);
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            bmpCanvas.drawCircle(centerX, centerY, bmpCanvas.getWidth() / 2 - (TICK_OFFSET + TICK_LENGTH * 2) * displayDensity, maskPaint);
            MASK_BMP_PAINT = new Paint();
            MASK_BMP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        }
        canvas.drawBitmap(MASK_BMP, 0, 0, MASK_BMP_PAINT);

        canvas.save();
        //set the initial rotation based on our given angle and azimuth
        canvas.rotate(azimuth + baseAngle, centerX, centerY);

        //draw ticks
        canvas.save();
        final int totalDegrees = 360;
        for (int i = 0; i < NUM_TICKS; i++) {
            canvas.rotate(totalDegrees / NUM_TICKS, centerX, centerY);
            canvas.drawLine(centerX, TICK_OFFSET * displayDensity, centerX, (TICK_OFFSET + TICK_LENGTH) * displayDensity, paintTick);
        }
        canvas.restore();

        //draw arrow
        if (arrowPath == null) {
            arrowPath = new Path();
            arrowPath.moveTo(centerX, 0);
            arrowPath.lineTo(centerX + ARROW_WIDTH * displayDensity / 2, (ARROW_HEIGHT - ARROW_FRUSTUM) * displayDensity);
            arrowPath.lineTo(centerX + ARROW_WIDTH * displayDensity / 2, ARROW_HEIGHT * displayDensity);
            arrowPath.lineTo(centerX - ARROW_WIDTH * displayDensity / 2, ARROW_HEIGHT * displayDensity);
            arrowPath.lineTo(centerX - ARROW_WIDTH * displayDensity / 2, (ARROW_HEIGHT - ARROW_FRUSTUM) * displayDensity);
            arrowPath.close();
        }
        canvas.drawPath(arrowPath, paintArrow);

        //draw bottom tick
        canvas.drawLine(centerX, h - (TICK_OFFSET + TICK_LENGTH) * displayDensity, centerX, h - (TICK_OFFSET - TICK_LENGTH) * displayDensity, paintTick);
        canvas.restore();
    }

}
