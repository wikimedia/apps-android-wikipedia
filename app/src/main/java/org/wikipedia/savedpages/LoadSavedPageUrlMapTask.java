package org.wikipedia.savedpages;

import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.concurrency.SaneAsyncTask;

/** To load the file with the image source URL mappings in the background. */
public class LoadSavedPageUrlMapTask extends SaneAsyncTask<JSONObject> {
    private final PageTitle title;

    public LoadSavedPageUrlMapTask(PageTitle title) {
        this.title = title;
    }

    @Override
    public JSONObject performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        return savedPage.readUrlMapFromFileSystem();
    }
}
