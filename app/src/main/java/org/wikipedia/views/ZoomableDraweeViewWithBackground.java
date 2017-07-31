package org.wikipedia.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.facebook.samples.zoomable.DefaultZoomableController;
import com.facebook.samples.zoomable.ZoomableDraweeView;

public class ZoomableDraweeViewWithBackground extends ZoomableDraweeView {
    private final Paint backgroundPaint = new Paint();
    private boolean drawBackground;

    public ZoomableDraweeViewWithBackground(Context context) {
        super(context);
        init();
    }

    public ZoomableDraweeViewWithBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZoomableDraweeViewWithBackground(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setDrawBackground(boolean draw) {
        drawBackground = draw;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawBackground) {
            DefaultZoomableController controller = (DefaultZoomableController) getZoomableController();
            int saveCount = canvas.save();
            canvas.concat(controller.getTransform());
            canvas.drawRect(controller.getImageBounds(), backgroundPaint);
            canvas.restoreToCount(saveCount);
        }
        super.onDraw(canvas);
    }

    private void init() {
        backgroundPaint.setColor(Color.WHITE);
    }
}
