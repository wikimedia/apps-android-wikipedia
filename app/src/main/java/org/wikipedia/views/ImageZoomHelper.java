package org.wikipedia.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Based on:
 * https://github.com/okaybroda/ImageZoom/blob/master/library/src/main/java/com/viven/imagezoom/ImageZoomHelper.java
 */
public class ImageZoomHelper {
    private static int FLAG_ZOOMABLE = 1;
    private static int FLAG_UNZOOMABLE = 2;

    private View zoomableView = null;
    private ViewGroup parentOfZoomableView;
    private ViewGroup.LayoutParams zoomableViewLP;
    private FrameLayout.LayoutParams zoomableViewFrameLP;
    private int viewIndex;
    private View darkView;
    private double originalDistance;
    private int[] twoPointCenter;
    private int[] originalXY;
    private int pivotX = 0, pivotY = 0;
    private WeakReference<Activity> activityWeakReference;
    private ViewGroup decorView;

    private boolean isAnimatingDismiss = false;

    private List<OnZoomListener> zoomListeners = new ArrayList<>();

    public ImageZoomHelper(Activity activity) {
        this.activityWeakReference = new WeakReference<>(activity);
    }

    @SuppressWarnings("checkstyle:methodlength")
    public boolean onDispatchTouchEvent(MotionEvent ev) {
        Activity activity = activityWeakReference.get();
        if (activity == null) {
            return false;
        }

        if (ev.getPointerCount() == 2) {
            if (zoomableView == null) {
                View view = findZoomableView(ev, activity.findViewById(android.R.id.content));
                if (view != null) {
                    zoomableView = view;

                    // get view's original location relative to the window
                    originalXY = new int[2];
                    view.getLocationInWindow(originalXY);

                    // this FrameLayout will be the zoomableView's temporary parent
                    FrameLayout frameLayout = new FrameLayout(view.getContext());

                    // this view is to gradually darken the backdrop as user zooms
                    darkView = new View(view.getContext());
                    darkView.setBackgroundColor(Color.BLACK);
                    darkView.setAlpha(0f);

                    // adding darkening backdrop to the frameLayout
                    frameLayout.addView(darkView, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));

                    decorView = (ViewGroup)activity.getWindow().getDecorView();
                    decorView.addView(frameLayout);

                    // get the parent of the zoomable view and get its index and layout param
                    parentOfZoomableView = (ViewGroup) zoomableView.getParent();
                    viewIndex = parentOfZoomableView.indexOfChild(zoomableView);
                    this.zoomableViewLP = zoomableView.getLayoutParams();

                    // this is the new layout param for the zoomableView
                    zoomableViewFrameLP = new FrameLayout.LayoutParams(
                            view.getWidth(), view.getHeight());
                    zoomableViewFrameLP.leftMargin = originalXY[0];
                    zoomableViewFrameLP.topMargin = originalXY[1];

                    // zoomableView has to be removed from parent view before being added to its new parent
                    parentOfZoomableView.removeView(zoomableView);
                    frameLayout.addView(zoomableView, zoomableViewFrameLP);

                    // Pointer variables to store the original touch positions
                    MotionEvent.PointerCoords pointerCoords1 = new MotionEvent.PointerCoords();
                    ev.getPointerCoords(0, pointerCoords1);

                    MotionEvent.PointerCoords pointerCoords2 = new MotionEvent.PointerCoords();
                    ev.getPointerCoords(1, pointerCoords2);

                    // storing distance between the two positions to be compared later on for zooming
                    originalDistance = (int) getDistance(pointerCoords1.x, pointerCoords2.x,
                            pointerCoords1.y, pointerCoords2.y);

                    // storing center point of the two pointers to move the view according to the touch position
                    twoPointCenter = new int[]{
                            (int) ((pointerCoords2.x + pointerCoords1.x) / 2),
                            (int) ((pointerCoords2.y + pointerCoords1.y) / 2)
                    };

                    //storing pivot point for zooming image from its touch coordinates
                    pivotX = (int) ev.getRawX() - originalXY[0];
                    pivotY = (int) ev.getRawY() - originalXY[1];

                    sendZoomEventToListeners(zoomableView, true);
                    return true;
                }
            } else {
                MotionEvent.PointerCoords pointerCoords1 = new MotionEvent.PointerCoords();
                ev.getPointerCoords(0, pointerCoords1);

                MotionEvent.PointerCoords pointerCoords2 = new MotionEvent.PointerCoords();
                ev.getPointerCoords(1, pointerCoords2);

                int[] newCenter = new int[]{
                        (int) ((pointerCoords2.x + pointerCoords1.x) / 2),
                        (int) ((pointerCoords2.y + pointerCoords1.y) / 2)
                };

                int currentDistance = (int) getDistance(pointerCoords1.x, pointerCoords2.x,
                        pointerCoords1.y, pointerCoords2.y);
                double pctIncrease = (currentDistance - originalDistance) / originalDistance;

                zoomableView.setPivotX(pivotX);
                zoomableView.setPivotY(pivotY);

                zoomableView.setScaleX((float) (1 + pctIncrease));
                zoomableView.setScaleY((float) (1 + pctIncrease));

                updateZoomableViewMargins(newCenter[0] - twoPointCenter[0] + originalXY[0],
                        newCenter[1] - twoPointCenter[1] + originalXY[1]);

                final int step = 8;
                darkView.setAlpha((float) (pctIncrease / step));
                return true;
            }
        } else {
            if (zoomableView != null && !isAnimatingDismiss) {
                isAnimatingDismiss = true;

                final float scaleYStart = zoomableView.getScaleY();
                final float scaleXStart = zoomableView.getScaleX();
                final int leftMarginStart = zoomableViewFrameLP.leftMargin;
                final int topMarginStart = zoomableViewFrameLP.topMargin;
                final float alphaStart = darkView.getAlpha();

                final float scaleYEnd = 1f;
                final float scaleXEnd = 1f;
                final int leftMarginEnd = originalXY[0];
                final int topMarginEnd = originalXY[1];
                final float alphaEnd = 0f;

                final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
                valueAnimator.setDuration(activity.getResources().getInteger(android.R.integer.config_shortAnimTime));
                valueAnimator.addUpdateListener(animator -> {
                    float animatedFraction = animator.getAnimatedFraction();
                    if (zoomableView != null) {
                        updateZoomableView(animatedFraction, scaleYStart, scaleXStart,
                                leftMarginStart, topMarginStart,
                                scaleXEnd, scaleYEnd, leftMarginEnd, topMarginEnd);
                    }

                    if (darkView != null) {
                        darkView.setAlpha(((alphaEnd - alphaStart) * animatedFraction) + alphaStart);
                    }
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        end();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        end();
                    }

                    void end() {
                        if (zoomableView != null) {
                            updateZoomableView(1f, scaleYStart, scaleXStart,
                                    leftMarginStart, topMarginStart,
                                    scaleXEnd, scaleYEnd, leftMarginEnd, topMarginEnd);
                        }
                        dismissDialogAndViews();

                        valueAnimator.removeAllListeners();
                        valueAnimator.removeAllUpdateListeners();
                    }
                });
                valueAnimator.start();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void updateZoomableView(float animatedFraction, float scaleYStart,
                                    float scaleXStart, int leftMarginStart,
                                    int topMarginStart, float scaleXEnd, float scaleYEnd,
                                    int leftMarginEnd, int topMarginEnd) {
        zoomableView.setScaleX(((scaleXEnd - scaleXStart) * animatedFraction) + scaleXStart);
        zoomableView.setScaleY(((scaleYEnd - scaleYStart) * animatedFraction) + scaleYStart);

        updateZoomableViewMargins(
                ((leftMarginEnd - leftMarginStart) * animatedFraction) + leftMarginStart,
                ((topMarginEnd - topMarginStart) * animatedFraction) + topMarginStart);
    }

    private void updateZoomableViewMargins(float left, float top) {
        if (zoomableView != null && zoomableViewFrameLP != null) {
            zoomableViewFrameLP.leftMargin = (int) left;
            zoomableViewFrameLP.topMargin = (int) top;
            zoomableView.setLayoutParams(zoomableViewFrameLP);
        }
    }

    /**
     * Dismiss dialog and set views to null for garbage collection
     */
    private void dismissDialogAndViews() {
        sendZoomEventToListeners(zoomableView, false);
        if (zoomableView != null) {
            zoomableView.setVisibility(View.VISIBLE);
            ViewGroup parent = (ViewGroup) zoomableView.getParent();
            parent.removeView(zoomableView);
            this.parentOfZoomableView.addView(zoomableView, viewIndex, zoomableViewLP);
        }
        decorView = null;
        darkView = null;
        resetOriginalViewAfterZoom();
        isAnimatingDismiss = false;
    }

    public void addOnZoomListener(OnZoomListener onZoomListener) {
        zoomListeners.add(onZoomListener);
    }

    public void removeOnZoomListener(OnZoomListener onZoomListener) {
        zoomListeners.remove(onZoomListener);
    }

    private void sendZoomEventToListeners(View zoomableView, boolean zoom) {
        for (OnZoomListener onZoomListener : zoomListeners) {
            if (zoom) {
                onZoomListener.onImageZoomStarted(zoomableView);
            } else {
                onZoomListener.onImageZoomEnded(zoomableView);
            }
        }
    }

    private void resetOriginalViewAfterZoom() {
        if (zoomableView != null) {
            zoomableView.invalidate();
            zoomableView = null;
        }
    }

    /**
     * Get distance between two points
     *
     * @param x1 distance x from
     * @param x2 distance x end
     * @param y1 distance y from
     * @param y2 distance y end
     * @return distance
     */
    private double getDistance(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Finds the view that has the R.id.zoomable tag and also contains the x and y coordinations
     * of two pointers
     *
     * @param event MotionEvent that contains two pointers
     * @param view  View to find in
     * @return zoomable View
     */
    private View findZoomableView(MotionEvent event, View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();

            MotionEvent.PointerCoords pointerCoords1 = new MotionEvent.PointerCoords();
            event.getPointerCoords(0, pointerCoords1);

            MotionEvent.PointerCoords pointerCoords2 = new MotionEvent.PointerCoords();
            event.getPointerCoords(1, pointerCoords2);

            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i);

                if ((getIntTag(child) & FLAG_UNZOOMABLE) == 0) {
                    Rect visibleRect = new Rect();
                    int[] location = new int[2];
                    child.getLocationOnScreen(location);
                    visibleRect.left = location[0];
                    visibleRect.top = location[1];
                    visibleRect.right = visibleRect.left + child.getWidth();
                    visibleRect.bottom = visibleRect.top + child.getHeight();

                    if (visibleRect.contains((int) pointerCoords1.x, (int) pointerCoords1.y)
                            && visibleRect.contains((int) pointerCoords2.x, (int) pointerCoords2.y)) {
                        return (getIntTag(child) & FLAG_ZOOMABLE) != 0 ? child : findZoomableView(event, child);
                    }
                }
            }
        }

        return null;
    }

    private static int getIntTag(View view) {
        return view.getTag() == null ? 0 : (int)view.getTag();
    }

    public static void setViewZoomable(View view) {
        view.setTag(getIntTag(view) | FLAG_ZOOMABLE);
    }

    public static void clearViewZoomable(View view) {
        view.setTag(getIntTag(view) & ~FLAG_ZOOMABLE);
    }

    public interface OnZoomListener {
        void onImageZoomStarted(View view);
        void onImageZoomEnded(View view);
    }
}
