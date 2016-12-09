package com.facebook.samples.zoomable;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.facebook.samples.gestures.TransformGestureDetector;

//Double tap on gallery to zoom-in.
final class DoubleTapZoomableController extends DefaultZoomableController implements GestureDetector.OnGestureListener,
                                                                               GestureDetector.OnDoubleTapListener {

	private GestureDetector mGestureDetector;

	DoubleTapZoomableController(TransformGestureDetector gestureDetector, Context context) {
		super(gestureDetector);
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setOnDoubleTapListener(this);
	}

	static DoubleTapZoomableController newInstance(Context context) {
		return new DoubleTapZoomableController(TransformGestureDetector.newInstance(), context);
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		PointF vp = mapViewToImage(new PointF(event.getX(), event.getY()));
		PointF ip = mapViewToImage(vp);
		if (getScaleFactor() > 1.0f) {
			zoomToPoint(1.0f, ip, vp);
		} else {
			zoomToPoint(2.0f, ip, vp);
		}
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		return false;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent event) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent event, MotionEvent event1, float v, float v1) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent event) {
	}

	@Override
	public boolean onFling(MotionEvent event, MotionEvent event1, float v, float v1) {
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isEnabled()) {
			mGestureDetector.onTouchEvent(event);
			return super.onTouchEvent(event);
		}
		return false;
	}

}