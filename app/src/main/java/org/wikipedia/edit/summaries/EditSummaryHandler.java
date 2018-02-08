package org.wikipedia.edit.summaries;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.EditHistoryContract;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ContentProviderClientCompat;

import java.util.Date;

import static org.wikipedia.util.L10nUtil.setConditionalTextDirection;

public class EditSummaryHandler {
    private final View container;
    private final AutoCompleteTextView summaryEdit;

    public EditSummaryHandler(@NonNull final View container,
                              @NonNull AutoCompleteTextView summaryEditText,
                              @NonNull PageTitle title) {
        this.container = container;
        this.summaryEdit = summaryEditText;

        this.container.setOnClickListener((view) -> summaryEdit.requestFocus());

        EditSummaryAdapter adapter = new EditSummaryAdapter(container.getContext(), null, true);
        summaryEdit.setAdapter(adapter);
        adapter.setFilterQueryProvider((charSequence) -> {
            ContentProviderClient client = EditSummary.DATABASE_TABLE
                    .acquireClient(container.getContext().getApplicationContext());
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
                ContentProviderClientCompat.close(client);
            }
        });

        setConditionalTextDirection(summaryEdit, title.getWikiSite().languageCode());
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
            return LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
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
