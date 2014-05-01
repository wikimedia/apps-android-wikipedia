package org.wikipedia.migration;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataMigrator {
    Context context;

    public DataMigrator(Context context) {
        super();
        this.context = context;
    }

    /**
     * @return Are databases from the old app present?
     */
    public boolean hasData() {
        String path = localDocumentPath("savedPagesDB.db");
        File file = new File(path);
        return file.exists();
    }

    /**
     * Extract the high-level list of saved pages from old app, without their actual saved data.
     *
     * @return list of JSONObject dictionaries containing 'title' and 'lang' pairs
     */
    public List<JSONObject> extractSavedPages() {
        ArrayList<JSONObject> arr = new ArrayList<JSONObject>();

        for (String jsonString : fetchRawSavedPages()) {
            try {
                JSONObject dict = new JSONObject(jsonString);
                arr.add(dict);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return arr;
    }

    public void removeOldData() {
        String path = localDocumentPath("savedPagesDB.db");
        File file = new File(path);
        file.delete();
    }

    // Private methods

    private String localDocumentPath(String local) {
        File basedir = context.getDir("database", Context.MODE_PRIVATE);
        String basePath = basedir.getAbsolutePath();

        return basePath + ":" + local;
    }

    private List<String> fetchRawSavedPages() {
        SQLiteDatabase savedPagesDB = SQLiteDatabase.openDatabase(localDocumentPath("savedPagesDB.db"), null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = savedPagesDB.query(
                "savedPagesDB", /* table */
                new String[] {"value"}, /* columns */
                "1", /* selection */
                null, /* selectArgs */
                null, /* groupBy */
                null, /* having */
                null, /* orderBy */
                null  /* limit */
        );

        ArrayList<String> arr = new ArrayList<String>();
        while (cursor.moveToNext()) {
            arr.add(cursor.getString(0));
        }
        cursor.close();

        return arr;
    }
}
