package org.wikipedia.richtext;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class TextViewSpanOnTouchListener implements View.OnTouchListener {
    @NonNull private TextView textView;

    public TextViewSpanOnTouchListener(@NonNull TextView textView) {
        this.textView = textView;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        ClickSpan span = getSpanned() == null ? null : getEventClickSpan(getSpanned(), event);
        if (span == null) {
            return false;
        }

        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            span.onClick(textView);
        }

        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN;
    }

    @Nullable
    private ClickSpan getEventClickSpan(@NonNull Spanned spanned, @NonNull MotionEvent event) {
        return getEventSpan(spanned, event, ClickSpan.class);
    }

    @Nullable
    private <T> T getEventSpan(@NonNull Spanned spanned,
                               @NonNull MotionEvent event,
                               @NonNull Class<T> clazz) {
        Integer offset = getEventCharacterOffset(event);
        if (offset == null) {
            return null;
        }

        T[] spans = spanned.getSpans(offset, offset, clazz);
        return spans.length > 0 ? spans[0] : null;
    }

    @Nullable
    private Integer getEventCharacterOffset(@NonNull MotionEvent event) {
        int x = getEventX(event);
        int y = getEventY(event);
        int line = getLineOffset(y);

        Rect bounds = getLineBounds(line);
        return bounds.contains(x, y) ? getCharacterOffset(line, x) : null;
    }

    private int getEventX(@NonNull MotionEvent event) {
        return Math.round(event.getX()) - getTotalPaddingLeft() + getScrollX();
    }

    private int getEventY(@NonNull MotionEvent event) {
        return Math.round(event.getY()) - getTotalPaddingTop() + getScrollY();
    }

    @Nullable
    private Spanned getSpanned() {
        return getText() instanceof Spanned ? (Spanned) getText() : null;
    }

    private CharSequence getText() {
        return textView.getText();
    }

    private Rect getLineBounds(int line) {
        Rect bounds = new Rect();
        getLayout().getLineBounds(line, bounds);
        // Left and right are set to the bounds of the TextView, not the bounds of the line. See
        // Layout.getLineBounds.
        bounds.left = Math.round(getLayout().getLineLeft(line));
        bounds.right = Math.round(getLayout().getLineRight(line));
        return bounds;
    }

    private int getCharacterOffset(int line, int x) {
        return getLayout().getOffsetForHorizontal(line, x);
    }

    private int getLineOffset(int y) {
        return getLayout().getLineForVertical(y);
    }

    private Layout getLayout() {
        return textView.getLayout();
    }

    private int getScrollX() {
        return textView.getScrollX();
    }

    private int getScrollY() {
        return textView.getScrollY();
    }

    private int getTotalPaddingLeft() {
        return textView.getTotalPaddingLeft();
    }

    private int getTotalPaddingTop() {
        return textView.getTotalPaddingTop();
    }
}