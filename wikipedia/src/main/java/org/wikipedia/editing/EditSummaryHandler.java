package org.wikipedia.editing;

import android.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.wikipedia.*;

public class EditSummaryHandler {
    private final Activity activity;
    private final View container;
    private final EditText summary_edit;

    public EditSummaryHandler(Activity activity) {
        this.activity = activity;

        container = activity.findViewById(R.id.group_edit_summary_container);
        summary_edit = (EditText)activity.findViewById(R.id.group_edit_summary_edit);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                summary_edit.requestFocus();
            }
        });
    }

    public void show() {
        container.setVisibility(View.VISIBLE);
    }

    public String getSummary(String sectionHeader) {
        if (TextUtils.isEmpty(sectionHeader)) {
            return summary_edit.getText().toString();
        } else {
            return "/* " + sectionHeader + " */ " + summary_edit.getText().toString();
        }
    }

    public boolean handleBackPressed() {
        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(View.GONE);
            return true;
        }
        return false;
    }
}
