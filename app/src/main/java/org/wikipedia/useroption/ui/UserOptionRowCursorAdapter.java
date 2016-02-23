package org.wikipedia.useroption.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.useroption.database.UserOptionRow;
import org.wikipedia.useroption.database.UserOptionDatabaseTable;

public class UserOptionRowCursorAdapter extends CursorAdapter {
    public UserOptionRowCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
        super(context, cursor, autoRequery);
    }

    @Override
    public UserOptionRowView newView(Context context, Cursor cursor, ViewGroup parent) {
        return new UserOptionRowView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        UserOptionRow item = UserOptionRow.DATABASE_TABLE.fromCursor(cursor);
        UserOptionRowView v = (UserOptionRowView) view;
        v.set(UserOptionDatabaseTable.Col.ID.val(cursor), item);
    }
}