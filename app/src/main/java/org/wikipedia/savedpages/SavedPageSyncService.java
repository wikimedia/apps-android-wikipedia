package org.wikipedia.savedpages;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SavedPageSyncService extends IntentService {
    @NonNull private ReadingListPageDao dao;

    public SavedPageSyncService() {
        super("SavedPageSyncService");
        dao = ReadingListPageDao.instance();
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        if (!DeviceUtil.isOnline(this)) {
            L.i("Device is offline; aborting sync service");
            return;
        }

        List<ReadingListPageDiskRow> queue = new ArrayList<>();
        Collection<ReadingListPageDiskRow> rows = dao.startDiskTransaction();
        L.i("Syncing saved rlp pages with saved pages service");

        for (final ReadingListPageDiskRow row : rows) {
            L.v("Found pending tx with status: " + row.status().name());
            switch (row.status()) {
                case UNSAVED:
                case DELETED:
                    continue;
                case OUTDATED:
                    queue.add(row);
                    continue;
                case ONLINE:
                case SAVED:
                    L.w("Received row with unexpected status " + row.status() + ": "
                            + row.toString());
                    continue;
                default:
                    throw new UnsupportedOperationException("Invalid disk row status: "
                            + row.status().name());
            }
        }
        saveNewEntries(queue);
    }

    @VisibleForTesting
    public void saveNewEntries(List<ReadingListPageDiskRow> queue) {
        while (!queue.isEmpty()) {
            ReadingListPageDiskRow row = queue.get(0);
            boolean ok = savePageFor(row);
            if (!ok) {
                dao.failDiskTransaction(queue);
                break;
            }
            dao.completeDiskTransaction(row);
            queue.remove(row);
        }
    }

    @VisibleForTesting
    public boolean savePageFor(@NonNull ReadingListPageDiskRow row) {
        final PageTitle title = makeTitleFrom(row);
        if (title == null) {
            return false;
        }

        return false;
    }

    @Nullable
    private PageTitle makeTitleFrom(@NonNull ReadingListPageDiskRow row) {
        ReadingListPageRow pageRow = row.dat();
        if (pageRow == null) {
            return null;
        }
        String namespace = pageRow.namespace().toLegacyString();
        return new PageTitle(namespace, pageRow.title(), pageRow.wikiSite());
    }
}
