package org.wikipedia.readinglist;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import org.wikipedia.R;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;

import java.util.ArrayList;
import java.util.List;

public class RemoveFromReadingListsDialog {
    public interface Callback {
        void onDeleted(@NonNull ReadingListPage page);
    }

    @NonNull private final ReadingListPage page;

    public RemoveFromReadingListsDialog(@NonNull ReadingListPage page) {
        this.page = page;
    }

    public void deleteOrShowDialog(@NonNull Context context, @Nullable Callback callback) {
        if (page.listKeys().isEmpty()) {
            return;
        }
        if (page.listKeys().size() == 1) {
            ReadingListPageDao.instance().deletePageFromLists(page, page.listKeys());
            if (callback != null) {
                callback.onDeleted(page);
            }
            return;
        }
        showDialog(context, callback);
    }

    private void showDialog(@NonNull Context context, @Nullable final Callback callback) {
        final String[] listKeys = page.listKeys().toArray(new String[]{});
        final String[] listNames = new String[listKeys.length];
        final boolean[] selected = new boolean[listNames.length];

        for (int i = 0; i < listKeys.length; i++) {
            listNames[i] = ReadingListDaoProxy.listName(listKeys[i]);
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.reading_list_remove_from_lists)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<String> selectedKeys = new ArrayList<>();
                        for (int i = 0; i < listNames.length; i++) {
                            if (selected[i]) {
                                selectedKeys.add(listKeys[i]);
                            }
                        }
                        if (!selectedKeys.isEmpty()) {
                            ReadingListPageDao.instance().deletePageFromLists(page, selectedKeys);
                            if (callback != null) {
                                callback.onDeleted(page);
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(listNames, selected, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean checked) {
                        selected[which] = checked;
                    }
                })
                .create()
                .show();
    }
}
