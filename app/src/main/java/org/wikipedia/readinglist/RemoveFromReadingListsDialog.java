package org.wikipedia.readinglist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import org.wikipedia.R;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;

import java.util.Collections;
import java.util.List;

public class RemoveFromReadingListsDialog {
    public interface Callback {
        void onDeleted(@NonNull ReadingListPage page);
    }

    @Nullable private List<ReadingList> listsContainingPage;

    public RemoveFromReadingListsDialog(@NonNull List<ReadingList> listsContainingPage) {
        this.listsContainingPage = listsContainingPage;
    }

    public void deleteOrShowDialog(@NonNull Context context, @Nullable Callback callback) {
        if (listsContainingPage == null || listsContainingPage.isEmpty()) {
            return;
        }
        if (listsContainingPage.size() == 1 && !listsContainingPage.get(0).pages().isEmpty()) {
            ReadingListDbHelper.instance().markPagesForDeletion(listsContainingPage.get(0),
                    Collections.singletonList(listsContainingPage.get(0).pages().get(0)));
            if (callback != null) {
                callback.onDeleted(listsContainingPage.get(0).pages().get(0));
            }
            return;
        }
        showDialog(context, callback);
    }

    private void showDialog(@NonNull Context context, @Nullable final Callback callback) {
        final String[] listNames = new String[listsContainingPage.size()];
        final boolean[] selected = new boolean[listNames.length];

        for (int i = 0; i < listsContainingPage.size(); i++) {
            listNames[i] = listsContainingPage.get(i).title();
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.reading_list_remove_from_lists)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    boolean atLeastOneSelected = false;
                    for (int i = 0; i < listNames.length; i++) {
                        if (selected[i]) {
                            atLeastOneSelected = true;
                            ReadingListDbHelper.instance().markPagesForDeletion(listsContainingPage.get(i),
                                    Collections.singletonList(listsContainingPage.get(i).pages().get(0)));
                        }
                    }
                    if (callback != null && atLeastOneSelected) {
                        callback.onDeleted(listsContainingPage.get(0).pages().get(0));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(listNames, selected, (dialog, which, checked) -> selected[which] = checked)
                .create()
                .show();
    }
}
