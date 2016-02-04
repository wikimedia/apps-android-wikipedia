package org.wikipedia.editing.summaries;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

import static org.wikipedia.util.L10nUtil.setConditionalTextDirection;

import java.util.Date;

public class EditSummaryHandler {
    private final Activity activity;
    private final View container;
    private final AutoCompleteTextView summaryEdit;

    public EditSummaryHandler(final Activity activity, View container,
                              AutoCompleteTextView summaryEditText, PageTitle title) {
        this.activity = activity;
        this.container = container;
        this.summaryEdit = summaryEditText;

        this.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                summaryEdit.requestFocus();
            }
        });

        EditSummaryAdapter adapter = new EditSummaryAdapter(activity, null, true);
        summaryEdit.setAdapter(adapter);
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                ContentProviderClient client = EditSummary.DATABASE_TABLE.acquireClient(activity);
                try {
                    return client.query(
                            EditSummary.DATABASE_TABLE.getBaseContentURI(),
                            null,
                            "summary LIKE ?",
                            new String[] {charSequence + "%"},
                            "lastUsed DESC");
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                } finally {
                    client.release();
                }
            }
        });

        setConditionalTextDirection(summaryEdit, title.getSite().getLanguageCode());
    }

    public void show() {
        container.setVisibility(View.VISIBLE);
    }

    public void persistSummary() {
        WikipediaApp app = (WikipediaApp)container.getContext().getApplicationContext();
        EditSummary summary = new EditSummary(summaryEdit.getText().toString(), new Date());
        app.getDatabaseClient(EditSummary.class).upsert(summary, EditSummaryDatabaseTable.SELECTION_KEYS);
    }

    public boolean handleBackPressed() {
        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    private  class EditSummaryAdapter extends android.support.v4.widget.CursorAdapter {
        EditSummaryAdapter(Context context, Cursor c, boolean autoRequery) {
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
            return EditSummary.DATABASE_TABLE.fromCursor(cursor).getSummary();
        }
    }
}
