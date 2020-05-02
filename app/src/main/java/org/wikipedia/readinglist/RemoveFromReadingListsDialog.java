package org.wikipedia.readinglist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.wikipedia.R;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.IterableUtils.first;
import static org.apache.commons.collections4.IterableUtils.get;
import static org.apache.commons.collections4.IterableUtils.size;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class RemoveFromReadingListsDialog {
    public interface Callback {
        void onDeleted(@NonNull List<ReadingList> lists, @NonNull ReadingListPage page);
    }

    @Nullable private List<ReadingList> listsContainingPage;

    public RemoveFromReadingListsDialog(@NonNull List<ReadingList> listsContainingPage) {
        this.listsContainingPage = listsContainingPage;
        ReadingList.sort(listsContainingPage, ReadingList.SORT_BY_NAME_ASC);
    }

    public void deleteOrShowDialog(@NonNull Context context, @Nullable Callback callback) {
        if (isEmpty(listsContainingPage)) {
            return;
        }
        if (size(listsContainingPage) == 1) {
            final ReadingList first = first(listsContainingPage);
            if (!first.pages().isEmpty()) {
                ReadingListDbHelper.instance().markPagesForDeletion(first,
                        Collections.singletonList(first.pages().get(0)));
                if (callback != null) {
                    callback.onDeleted(emptyIfNull(listsContainingPage), first.pages().get(0));
                }
            }
            return;
        }
        showDialog(context, callback);
    }

    private void showDialog(@NonNull Context context, @Nullable final Callback callback) {
        final String[] listNames = new String[size(listsContainingPage)];
        final boolean[] selected = new boolean[listNames.length];

        for (int i = 0; i < size(listsContainingPage); i++) {
            listNames[i] = get(listsContainingPage, i).title();
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.reading_list_remove_from_lists)
                .setPositiveButton(R.string.reading_list_remove_list_dialog_ok_button_text, (dialog, which) -> {
                    boolean atLeastOneSelected = false;
                    List<ReadingList> newLists = new ArrayList<>();
                    for (int i = 0; i < listNames.length; i++) {
                        final ReadingList list = get(listsContainingPage, i);
                        if (selected[i]) {
                            atLeastOneSelected = true;
                            ReadingListDbHelper.instance().markPagesForDeletion(list,
                                    Collections.singletonList(list.pages().get(0)));
                            newLists.add(list);
                        }
                    }
                    if (callback != null && atLeastOneSelected) {
                        callback.onDeleted(newLists, first(listsContainingPage).pages().get(0));
                    }
                })
                .setNegativeButton(R.string.reading_list_remove_from_list_dialog_cancel_button_text, null)
                .setMultiChoiceItems(listNames, selected, (dialog, which, checked) -> selected[which] = checked)
                .create()
                .show();
    }
}
