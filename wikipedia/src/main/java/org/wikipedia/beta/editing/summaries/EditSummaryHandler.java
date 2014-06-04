package org.wikipedia.beta.editing.summaries;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.R;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;

import java.util.Date;

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

            adapter = new EditSummaryAdapter(activity, null, true);
            summaryEdit.setAdapter(adapter);

            adapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence charSequence) {
                    ContentProviderClient client = activity.getContentResolver().acquireContentProviderClient(EditSummary.PERSISTANCE_HELPER.getBaseContentURI());
                    try {
                        return client.query(
                                EditSummary.PERSISTANCE_HELPER.getBaseContentURI(),
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
        } else {
            adapter = null;
        }

        Utils.setTextDirection(summaryEdit, title.getSite().getLanguage());
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
            return EditSummary.PERSISTANCE_HELPER.fromCursor(cursor).getSummary();
        }
    }
}
