package org.wikipedia.richtext;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TextViewSpanOnTouchListener implements View.OnTouchListener {
    @NonNull private TextView textView;

    public TextViewSpanOnTouchListener(@NonNull TextView textView) {
        this.textView = textView;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        List<ClickSpan> contains = getSpanned() == null
                ? null
                : filterContains(allClickSpans(getSpanned()), getEventPoint(event));
        if (contains == null || contains.isEmpty()) {
            return false;
        }


        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            for (ClickSpan span : contains) {
                span.onClick(textView);
            }
        }

        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN;
    }

    @NonNull
    private List<ClickSpan> filterContains(@NonNull ClickSpan[] spans, @NonNull PointF point) {
        List<ClickSpan> contains = new ArrayList<>();
        for (ClickSpan span : spans) {
            if (span.contains(point)) {
                contains.add(span);
            }
        }
        return contains;
    }

    @NonNull
    private ClickSpan[] allClickSpans(Spanned spanned) {
        return spanned.getSpans(0, spanned.length(), ClickSpan.class);
    }

    @NonNull
    private PointF getEventPoint(@NonNull MotionEvent event) {
        return new PointF(getEventX(event), getEventY(event));
    }

    private float getEventX(@NonNull MotionEvent event) {
        return event.getX() - getTotalPaddingLeft() + getScrollX();
    }

    private float getEventY(@NonNull MotionEvent event) {
        return event.getY() - getTotalPaddingTop() + getScrollY();
    }

    @Nullable
    private Spanned getSpanned() {
        return getText() instanceof Spanned ? (Spanned) getText() : null;
    }

    private CharSequence getText() {
        return textView.getText();
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