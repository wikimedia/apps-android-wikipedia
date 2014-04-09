package org.wikipedia.savedpages;

import android.content.*;
import org.json.*;
import org.wikipedia.*;
import org.wikipedia.data.*;
import org.wikipedia.page.*;

import java.io.*;

public class SavedPagePerister extends ContentPersister<SavedPage> {
    private final Context context;
    public SavedPagePerister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SavedPage.PERSISTANCE_HELPER.getBaseContentURI()
                ),
                SavedPage.PERSISTANCE_HELPER
        );
        this.context = context;
    }

    private String getSavePageName(PageTitle title) {
        return "savedpage-" + title.getHashedItentifier();
    }


    public void savePageContent(Page page) throws IOException {
        FileOutputStream out = context.openFileOutput(getSavePageName(page.getTitle()), Context.MODE_PRIVATE);
        try {
            out.write(page.toJSON().toString().getBytes("utf-8"));
        } catch (IOException e) {
            // Won't happen. EVER. JESUS CHRIST, Java!
            throw new RuntimeException(e);
        }
        out.close();
    }

    public Page loadPageContent(PageTitle title) throws IOException {
        FileInputStream in = context.openFileInput(getSavePageName(title));

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
        String contents = reader.readLine();
        if (reader.readLine() != null) {
            // Somehow we have a newline in there! THROW UP!
            throw new RuntimeException("Raw newline found in Saved Page for " + title);
        }
        reader.close();
        try {
            return new Page(new JSONObject(contents));
        } catch (JSONException e) {
            // This shouldn't happen, and if it does, it's an IOException
            throw new IOException(e);
        }
    }

    public boolean deletePageContent(PageTitle title) throws IOException {
        return context.deleteFile(getSavePageName(title));
    }
}
