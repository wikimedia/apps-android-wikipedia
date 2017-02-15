package org.wikipedia.readinglist.page.database.disk;

import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.async.AsyncDao;

import java.util.Collection;

public class DiskRowDao<Dat, Row extends DiskRow<Dat>> extends AsyncDao<DiskStatus, Dat, Row> {
    public DiskRowDao(@NonNull DatabaseClient<Row> client) {
        super(client);
    }

    public synchronized void markOnline(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        switch (query == null ? DiskStatus.SAVED : query.status()) {
            case SAVED:
            case OUTDATED:
            case DELETED:
                resetTransaction(row, DiskStatus.UNSAVED);
                break;
            case ONLINE:
            case UNSAVED:
                break;
            default:
                throw new RuntimeException("status=" + row.status());
        }
    }

    public synchronized void markOutdated(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        switch (query == null ? DiskStatus.ONLINE : query.status()) {
            case ONLINE:
            case SAVED:
            case UNSAVED:
            case DELETED:
                resetTransaction(row, DiskStatus.OUTDATED);
                break;
            case OUTDATED:
                break;
            default:
                throw new RuntimeException("status=" + row.status());
        }
    }

    public synchronized void markDeleted(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        switch (query == null ? DiskStatus.DELETED : query.status()) {
            case ONLINE:
            case SAVED:
            case OUTDATED:
            case UNSAVED:
                resetTransaction(row, DiskStatus.DELETED);
                break;
            case DELETED:
                break;
            default:
                throw new RuntimeException("status=" + row.status());
        }
    }

    @Override public synchronized boolean completeTransaction(@NonNull Row row, long timestamp) {
        if (super.completeTransaction(row, timestamp)) {
            if (row.status() == DiskStatus.DELETED) {
                delete(row);
            } else {
                upsert(row);
            }
            return true;
        }
        return false;
    }

    @Override public synchronized void clear() {
        final String selection = null;
        final String[] selectionArgs = null;
        Collection<Row> rows = query(selection, selectionArgs);
        for (Row row : rows) {
            delete(row);
        }
    }
}
