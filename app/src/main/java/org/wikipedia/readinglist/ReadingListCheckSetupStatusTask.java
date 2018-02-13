package org.wikipedia.readinglist;

import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.readinglist.sync.ReadingListClient;
import org.wikipedia.util.DateUtil;

import java.util.Date;

public class ReadingListCheckSetupStatusTask extends SaneAsyncTask<Void> {
    private ReadingListClient client;

    public ReadingListCheckSetupStatusTask() {
        client = new ReadingListClient(WikipediaApp.getInstance().getWikiSite());
    }

    @Override
    public Void performTask() throws Throwable {
        if (AccountUtil.isLoggedIn()) {
            client.getChangesSince(DateUtil.getIso8601DateFormat().format(new Date()));
        } else {
            throw new Exception("not-log-in");
        }
        return null;
    }
}
