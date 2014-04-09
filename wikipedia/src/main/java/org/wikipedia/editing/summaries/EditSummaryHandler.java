package org.wikipedia.editing.summaries;

import android.app.*;
import android.content.*;
import android.database.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.wikipedia.*;

import java.util.*;

public class EditSummaryHandler {
    private final Activity activity;
    private final View container;
    private final AutoCompleteTextView summaryEdit;
    private final EditSummaryAdapter adapter;

    public EditSummaryHandler(final Activity activity, PageTitle title) {
        this.activity = activity;

        container = activity.findViewById(R.id.group_edit_summary_container);
        summaryEdit = (AutoCompleteTextView)activity.findViewById(R.id.group_edit_summary_edit);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                summaryEdit.requestFocus();
            }
        });

        adapter = new EditSummaryAdapter(activity, null, true);
        summaryEdit.setAdapter(adapter);

        Utils.setTextDirection(summaryEdit, title.getSite().getLanguage());

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                ContentProviderClient client = activity.getContentResolver().acquireContentProviderClient(EditSummary.persistanceHelper.getBaseContentURI());
                try {
                    return client.query(
                            EditSummary.persistanceHelper.getBaseContentURI(),
                            null,
                            "summary LIKE ?",
                            new String[] {charSequence + "%"},
                            "lastUsed DESC");
                } catch (RemoteException e) {
                    // This shouldn't really be happening
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void show() {
        container.setVisibility(View.VISIBLE);
    }

    public String getSummary(String sectionHeader) {
        if (TextUtils.isEmpty(sectionHeader)) {
            return summaryEdit.getText().toString();
        } else {
            return "/* " + sectionHeader + " */ " + summaryEdit.getText().toString();
        }
    }

    public void persistSummary() {
        WikipediaApp app = (WikipediaApp)container.getContext().getApplicationContext();
        EditSummary summary = new EditSummary(summaryEdit.getText().toString(), new Date());
        app.getPersister(EditSummary.class).upsert(summary);
    }

    public boolean handleBackPressed() {
        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    private  class EditSummaryAdapter extends android.support.v4.widget.CursorAdapter {
        public EditSummaryAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View convertView, Context context, Cursor cursor) {
            ((TextView)convertView).setText(convertToString(cursor));
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            return EditSummary.persistanceHelper.fromCursor(cursor).getSummary();
        }
    }
}
