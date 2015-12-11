package org.wikipedia.richtext;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.widget.TextView;

public interface ClickSpan {
    void onClick(@NonNull TextView textView);
    /** @param point Click coordinates relative the host View. */
    boolean contains(@NonNull PointF point);
}