package org.wikipedia.editing.summaries;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.EditHistoryContract;
import org.wikipedia.page.PageTitle;

import java.util.Date;

import static org.wikipedia.util.L10nUtil.setConditionalTextDirection;

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
                Uri uri = EditHistoryContract.Summary.URI;
                final String[] projection = null;
                String selection = EditHistoryContract.Summary.SUMMARY.qualifiedName() + " like ?";
                String[] selectionArgs = new String[] {charSequence + "%"};
                String order = EditHistoryContract.Summary.ORDER_MRU;
                try {
                    return client.query(uri, projection, selection, selectionArgs, order);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                } finally {
                    client.release();
                }
            }
        });

        setConditionalTextDirection(summaryEdit, title.getSite().languageCode());
    }

    public void show() {
        container.setVisibility(View.VISIBLE);
    }

    public void persistSummary() {
        WikipediaApp app = (WikipediaApp)container.getContext().getApplicationContext();
        EditSummary summary = new EditSummary(summaryEdit.getText().toString(), new Date());
        app.getDatabaseClient(EditSummary.class).upsert(summary, EditHistoryContract.Summary.SELECTION);
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
