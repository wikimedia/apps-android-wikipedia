package org.wikipedia.readinglist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;

import java.util.List;

public class ReadingListBookmarkMenu {
    public interface Callback {
        void onAddRequest(@Nullable ReadingListPage page);
        void onDeleted(@Nullable ReadingListPage page);
    }

    @NonNull private final View anchorView;
    @Nullable private final Callback callback;
    @Nullable private List<ReadingList> listsContainingPage;

    public ReadingListBookmarkMenu(@NonNull View anchorView, @Nullable Callback callback) {
        this.anchorView = anchorView;
        this.callback = callback;
    }

    public void show(@NonNull PageTitle title) {
        CallbackTask.execute(() -> {
            List<ReadingListPage> pageOccurrences = ReadingListDbHelper.instance().getAllPageOccurrences(title);
            listsContainingPage = ReadingListDbHelper.instance().getListsFromPageOccurrences(pageOccurrences);
            return null;
        }, new CallbackTask.DefaultCallback<Void>() {
            @Override
            public void success(Void v) {
                if (!ViewCompat.isAttachedToWindow(anchorView)) {
                    return;
                }
                showMenu();
            }
        });
    }

    private void showMenu() {
        if (listsContainingPage == null || listsContainingPage.isEmpty()) {
            return;
        }
        Context context = anchorView.getContext();
        PopupMenu menu = new PopupMenu(context, anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_reading_list_page_toggle, menu.getMenu());
        menu.setOnMenuItemClickListener(new PageSaveMenuClickListener());
        if (listsContainingPage.size() == 1) {
            MenuItem removeItem = menu.getMenu().findItem(R.id.menu_remove_from_lists);
            removeItem.setTitle(context.getString(R.string.reading_list_remove_from_list, listsContainingPage.get(0).title()));
        }
        menu.show();
    }

    private void deleteOrShowDialog(@NonNull Context context) {
        if (listsContainingPage == null || listsContainingPage.isEmpty()) {
            return;
        }
        new RemoveFromReadingListsDialog(listsContainingPage).deleteOrShowDialog(context,
                page -> {
                    if (callback != null) {
                        callback.onDeleted(page);
                    }
                });
    }

    private class PageSaveMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_add_to_other_list:
                    if (callback != null && listsContainingPage != null && !listsContainingPage.isEmpty()) {
                        callback.onAddRequest(listsContainingPage.get(0).pages().get(0));
                    }
                    return true;
                case R.id.menu_remove_from_lists:
                    deleteOrShowDialog(anchorView.getContext());
                    return true;
                default:
                    return false;
            }
        }
    }
}
