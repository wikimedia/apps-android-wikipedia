package org.wikipedia.editing.summaries;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import org.wikipedia.R;

public class EditSummaryTag extends TextView {

    Resources resources;
    private boolean selected = false;

    public EditSummaryTag(Context context) {
        super(context);
        setupEditSummaryTag(context);
    }

    public EditSummaryTag(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupEditSummaryTag(context);
    }

    public EditSummaryTag(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupEditSummaryTag(context);
    }

    private void setupEditSummaryTag(Context context) {
        resources = context.getResources();

        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        int margin = getDp(4);
        int padding = getDp(8);
        params.setMargins(margin, margin, margin, margin);
        setLayoutParams(params);

        setPadding(padding, padding, padding, padding);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selected = !selected;
                updateState();
            }
        });

        updateState();
    }

    @Override
    public String toString() {
        return getText().toString();
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateState();
    }

    private void updateState() {
        setBackgroundResource(selected ? R.drawable.editpage_improve_tag_selected : R.drawable.editpage_improve_tag_unselected);
        setTextColor(resources.getColor(selected ? android.R.color.white : R.color.blue_progressive));
    }

    private int getDp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.getDisplayMetrics()
        );
    }
}