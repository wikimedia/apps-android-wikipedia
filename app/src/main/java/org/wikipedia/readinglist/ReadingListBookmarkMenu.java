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
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;

public class ReadingListBookmarkMenu {
    public interface Callback {
        void onAddRequest(@Nullable ReadingListPage page);
        void onDeleted(@Nullable ReadingListPage page);
    }

    @NonNull private final View anchorView;
    @Nullable private final Callback callback;
    @Nullable private ReadingListPage page;

    public ReadingListBookmarkMenu(@NonNull View anchorView, @Nullable Callback callback) {
        this.anchorView = anchorView;
        this.callback = callback;
    }

    public void show(@NonNull PageTitle title) {
        ReadingList.DAO.anyListContainsTitleAsync(ReadingListDaoProxy.key(title),
                new CallbackTask.DefaultCallback<ReadingListPage>() {
                    @Override public void success(@Nullable ReadingListPage page) {
                        if (!ViewCompat.isAttachedToWindow(anchorView)) {
                            return;
                        }
                        ReadingListBookmarkMenu.this.page = page;
                        showMenu();
                    }
                });
    }

    private void showMenu() {
        if (page == null) {
            return;
        }
        Context context = anchorView.getContext();
        PopupMenu menu = new PopupMenu(context, anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_reading_list_page_toggle, menu.getMenu());
        menu.setOnMenuItemClickListener(new PageSaveMenuClickListener());
        if (page.listKeys().size() == 1) {
            MenuItem removeItem = menu.getMenu().findItem(R.id.menu_remove_from_lists);
            removeItem.setTitle(context.getString(R.string.reading_list_remove_from_list,
                    ReadingListDaoProxy.listName((String) page.listKeys().toArray()[0])));
        }
        menu.show();
    }

    private void deleteOrShowDialog(@NonNull Context context) {
        if (page == null) {
            return;
        }
        new RemoveFromReadingListsDialog(page).deleteOrShowDialog(context,
                new RemoveFromReadingListsDialog.Callback() {
                    @Override
                    public void onDeleted(@NonNull ReadingListPage page) {
                        if (callback != null) {
                            callback.onDeleted(page);
                        }
                    }
                });
    }

    private class PageSaveMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_add_to_other_list:
                    if (callback != null) {
                        callback.onAddRequest(page);
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
