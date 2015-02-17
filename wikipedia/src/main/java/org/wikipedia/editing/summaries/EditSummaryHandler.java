package org.wikipedia.editing.summaries;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

import java.util.Date;

public class EditSummaryHandler {
    private final Activity activity;
    private final View container;
    private final AutoCompleteTextView summaryEdit;

    public EditSummaryHandler(final Activity activity, final View parent, PageTitle title) {
        this.activity = activity;
        container = parent.findViewById(R.id.edit_summary_container);
        summaryEdit = (AutoCompleteTextView)parent.findViewById(R.id.edit_summary_edit);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                summaryEdit.requestFocus();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // For some reason the autocomplete popup view crashes on
            // Gingerbread. This seems to be related to styles but I
            // can't quite figure out why.
            //
            // This call in AutoCompleteTextView.buildDropDown ends up failing:
            //
            //             mDropDownList.setSelector(mDropDownListHighlight);
            //
            // because mDropDownListHighlight seems to be null instead
            // of an expected drawable, and that ends up failing when used.

            final EditSummaryAdapter adapter = new EditSummaryAdapter(activity, null, true);
            summaryEdit.setAdapter(adapter);

            adapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence charSequence) {
                    ContentProviderClient client = activity.getContentResolver().acquireContentProviderClient(EditSummary.PERSISTENCE_HELPER

                                                                                                                      .getBaseContentURI());
                    try {
                        return client.query(
                                EditSummary.PERSISTENCE_HELPER.getBaseContentURI(),
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

        Utils.setTextDirection(summaryEdit, title.getSite().getLanguage());
    }

    public void show() {
        container.setVisibility(View.VISIBLE);
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
            return EditSummary.PERSISTENCE_HELPER.fromCursor(cursor).getSummary();
        }
    }
}
