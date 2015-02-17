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
        summaryEdit = (AutoCompleteTextView) parent.findViewById(R.id.edit_summary_edit);

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                summaryEdit.requestFocus();
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
            && WikipediaApp.getInstance().getCurrentTheme() == WikipediaApp.THEME_DARK) {
            // explicitly set text hint color
            summaryEdit.setHintTextColor(activity.getResources()
                .getColor(Utils.getThemedAttributeId(activity, R.attr.edit_text_color)));
        }
        EditSummaryAdapter adapter = new EditSummaryAdapter(activity, null, true);
        summaryEdit.setAdapter(adapter);
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                ContentProviderClient client = activity.getContentResolver()
                                       .acquireContentProviderClient(EditSummary.PERSISTENCE_HELPER
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
            View rootView = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                // explicitly set background color of the list item
                rootView.setBackgroundColor(activity.getResources().getColor(
                        Utils.getThemedAttributeId(activity, R.attr.window_background_color)));
            }
            return rootView;
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
