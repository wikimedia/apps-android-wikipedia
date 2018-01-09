package org.wikipedia.page.tabs;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;

public class TabsProvider {
    public interface TabsProviderListener {
        void onEnterTabView();
        void onCancelTabView();
        void onTabSelected(int position);
        void onNewTabRequested();
        void onCloseTabRequested(int position);
        void onCloseAllTabs();
    }

    public enum TabPosition {
        CURRENT_TAB,
        NEW_TAB_BACKGROUND,
        NEW_TAB_FOREGROUND,
        EXISTING_TAB
    }

    private PageFragment fragment;

    private View pageContentView;
    private View tabContainerView;
    private ListView tabListView;
    private TabListAdapter tabListAdapter;

    private ActionMode tabActionMode;
    // True when action mode is terminated by the side effect of creating a new tab or selecting an
    // existing tab.
    private boolean isActionModeDismissedIndirectly;

    private List<Tab> tabList;
    private boolean launchedExternally;

    @NonNull
    private TabsProviderListener providerListener = new DefaultTabsProviderListener();
    public void setTabsProviderListener(TabsProviderListener listener) {
        providerListener = DefaultTabsProviderListener.defaultIfNull(listener);
    }

    public TabsProvider(PageFragment fragment, List<Tab> tabList) {
        this.fragment = fragment;
        this.tabList = tabList;

        pageContentView = fragment.getView();
        tabContainerView = fragment.getTabsContainerView();
        tabListView = tabContainerView.findViewById(R.id.tabs_list);
        tabListAdapter = new TabListAdapter(fragment.getActivity().getLayoutInflater());
        tabListView.setAdapter(tabListAdapter);

        tabContainerView.setOnClickListener((v) -> {
            isActionModeDismissedIndirectly = true;
            providerListener.onCancelTabView();
        });

        tabListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            isActionModeDismissedIndirectly = true;
            providerListener.onTabSelected(position);
        });

    }

    public boolean onBackPressed() {
        if (isTabMode()) {
            exitTabMode();
            providerListener.onCancelTabView();
            return true;
        }
        return false;
    }

    public void enterTabMode(boolean launchedExternally) {
        enterTabMode(launchedExternally, null);
        providerListener.onEnterTabView();
    }

    public boolean shouldPopFragment() {
        return launchedExternally && !isActionModeDismissedIndirectly;
    }

    private boolean isTabMode() {
        return tabActionMode != null;
    }

    private void enterTabMode(boolean launchedExternally, @Nullable Runnable onTabModeEntered) {
        this.launchedExternally = launchedExternally;
        if (isTabMode()) {
            // already inside action mode...
            // but make sure to update the list of tabs.
            tabListAdapter.notifyDataSetInvalidated();
            tabListView.smoothScrollToPosition(tabList.size() - 1);
            if (onTabModeEntered != null) {
                onTabModeEntered.run();
            }
            return;
        }
        fragment.startSupportActionMode(new TabActionModeCallback(onTabModeEntered));
    }

    private class TabActionModeCallback implements ActionMode.Callback {
        private static final String TAB_ACTION_MODE_TAG = "actionModeTabList";
        private final Runnable onTabModeEntered;

        TabActionModeCallback(Runnable onTabModeEntered) {
            this.onTabModeEntered = onTabModeEntered;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            tabActionMode = mode;
            mode.getMenuInflater().inflate(R.menu.menu_tabs, menu);
            Animation anim = loadPageContentViewAnimation();
            fragment.getView().startAnimation(anim);
            layoutTabList(onTabModeEntered);

            return true;
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTag(TAB_ACTION_MODE_TAG);
            // find the action mode base view, and give it an empty click listener,
            // otherwise click events within the empty area of the action mode will be passed
            // down to the view beneath it, which is the Search bar, and we don't want to
            // unintentionally initiate Search.
            getActionBar().setClickable(true);

            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_new_tab:
                    isActionModeDismissedIndirectly = true;
                    providerListener.onNewTabRequested();
                    return true;
                case R.id.menu_close_all_tabs:
                    AlertDialog.Builder alert = new AlertDialog.Builder(fragment.getContext());
                    alert.setMessage(R.string.close_all_tabs_confirm);
                    alert.setPositiveButton(R.string.close_all_tabs_confirm_yes, (dialog, id) -> providerListener.onCloseAllTabs());
                    alert.setNegativeButton(R.string.close_all_tabs_confirm_no, null);
                    alert.create().show();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (!fragment.isAdded() || fragment.getContext() == null) {
                return;
            }
            Animation anim = AnimationUtils.loadAnimation(fragment.getContext(), R.anim.tab_list_zoom_exit);
            fragment.getView().startAnimation(anim);
            hideTabList();
            tabActionMode = null;
            fragment.showToolbar();
            if (!isActionModeDismissedIndirectly) {
                providerListener.onCancelTabView();
            }
        }

        @NonNull
        private View getActionBar() {
            return fragment.getActivity().findViewById(R.id.action_mode_bar);
        }
    }

    public void exitTabMode() {
        if (isTabMode()) {
            tabActionMode.finish();
        }
    }

    public void showAndHideTabs() {
        enterTabMode(false, new Runnable() {
            private final int animDelay = 500;

            @Override
            public void run() {
                tabContainerView.postDelayed(() -> {
                    isActionModeDismissedIndirectly = true;
                    exitTabMode();
                }, animDelay);
            }
        });
    }

    public void onConfigurationChanged() {
        if (isTabMode()) {
            setViewLayoutListener();
        }
    }

    private void layoutTabList(final Runnable onTabModeEntered) {
        tabContainerView.setVisibility(View.VISIBLE);
        tabListAdapter.notifyDataSetInvalidated();

        setTabListViewLayoutParams();

        Animation anim = loadTabListViewAnimation();
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (onTabModeEntered != null) {
                    onTabModeEntered.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tabListView.startAnimation(anim);
        // scroll to the bottom of the tab list
        tabListView.smoothScrollToPosition(tabList.size() - 1);
    }

    private void hideTabList() {
        Animation anim = AnimationUtils.loadAnimation(fragment.getContext(),
                                                      R.anim.tab_list_items_exit);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                tabContainerView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tabListView.startAnimation(anim);
    }

    public void invalidate() {
        tabListAdapter.notifyDataSetInvalidated();
    }

    private View.OnClickListener onItemCloseButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ViewHolder holder = (ViewHolder) v.getTag();
            final int position = holder.position;
            // if we're closing the last tab, don't do the animation...
            if (tabList.size() == 1) {
                providerListener.onCloseTabRequested(position);
            } else {
                Animation anim = AnimationUtils
                        .loadAnimation(fragment.getContext(), R.anim.slide_out_right);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) { }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        providerListener.onCloseTabRequested(position);
                    }

                    @Override public void onAnimationRepeat(Animation animation) { }
                });
                holder.container.startAnimation(anim);
            }
        }
    };

    // size the listview to be the same width as the scaled-down webview...
    private void setTabListViewLayoutParams() {
        final float proportionHorz = 0.15f;
        final float proportionVert = 0.4f;
        final int heightOffset = 16;
        int contentOffset = getContentTopOffsetPx(fragment.getContext());
        int maxHeight = (int) (pageContentView.getHeight() * proportionVert
                + pageContentView.getHeight() * proportionHorz
                - contentOffset - heightOffset * DimenUtil.getDensityScalar());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight);
        float margin = pageContentView.getWidth() * proportionHorz / 2f;
        params.leftMargin = (int) margin;
        params.rightMargin = (int) margin;
        params.topMargin = contentOffset;
        tabListView.setLayoutParams(params);
    }

    private void setViewLayoutListener() {
        tabListView.addOnLayoutChangeListener(new ViewLayoutListener());
    }

    private Animation loadPageContentViewAnimation() {
        return AnimationUtils.loadAnimation(fragment.getContext(), R.anim.tab_list_zoom_enter);
    }

    private Animation loadTabListViewAnimation() {
        return AnimationUtils.loadAnimation(fragment.getContext(), R.anim.tab_list_items_enter);
    }

    private class ViewLayoutListener implements View.OnLayoutChangeListener {
        @Override
        @SuppressWarnings("checkstyle:parameternumber")
        public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft,
                                   int oldTop, int oldRight, int oldBottom) {
            view.removeOnLayoutChangeListener(this);
            invalidateAnimations();
        }

        // Recalculate animations and apply the final frame.
        private void invalidateAnimations() {
            invalidatePageContentViewAnimation();
            invalidateTabListViewAnimation();
        }

        private void invalidatePageContentViewAnimation() {
            ViewUtil.setAnimationMatrix(pageContentView, loadPageContentViewAnimation());
        }

        private void invalidateTabListViewAnimation() {
            // Post the layout for update and animation _after_ the current layout is applied.
            tabListView.post(() -> {
                setTabListViewLayoutParams();
                ViewUtil.setAnimationMatrix(tabListView, loadTabListViewAnimation());
            });
        }
    }

    private static class DefaultTabsProviderListener implements TabsProviderListener {
        public static TabsProviderListener defaultIfNull(TabsProviderListener listener) {
            return listener == null ? new DefaultTabsProviderListener() : listener;
        }

        @Override public void onEnterTabView() { }
        @Override public void onCancelTabView() { }
        @Override public void onTabSelected(int position) { }
        @Override public void onNewTabRequested() { }
        @Override public void onCloseTabRequested(int position) { }
        @Override public void onCloseAllTabs() { }

    }

    private static class ViewHolder {
        private View container;
        private TextView title;
        private SimpleDraweeView thumbnail;
        private View gradient;
        private View closeButton;
        private int position;
    }

    private final class TabListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;

        TabListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public int getCount() {
            return tabList.size();
        }

        @Override
        public Tab getItem(int position) {
            return tabList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_tab_entry, parent, false);
                convertView.setTag(viewHolder);
                viewHolder.container = convertView;
                viewHolder.title = convertView.findViewById(R.id.tab_item_title);
                viewHolder.thumbnail = convertView.findViewById(R.id.tab_item_thumbnail);
                viewHolder.gradient = convertView.findViewById(R.id.tab_item_bottom_gradient);
                viewHolder.closeButton = convertView.findViewById(R.id.tab_item_close);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.position = position;
            viewHolder.closeButton.setTag(viewHolder);
            viewHolder.closeButton.setOnClickListener(onItemCloseButtonListener);
            // hide the shadow if this is the topmost tab
            viewHolder.gradient.setVisibility(
                    position == tabList.size() - 1 ? View.GONE : View.VISIBLE);

            @ColorRes int colorId = position == 0
                    ? R.color.base10
                    : getThemedAttributeId(fragment.getContext(), R.attr.material_theme_shadow);
            int color = ContextCompat.getColor(fragment.getContext(), colorId);

            // dynamically set the background color that will show through the rounded corners.
            // if it's the first last item in the tab list, we want the background to be the same
            // as the activity background, otherwise it should match the tab shadow color.
            convertView.setBackgroundColor(color);

            List<PageBackStackItem> backstack = tabList.get(position).getBackStack();
            if (backstack.size() > 0) {
                PageTitle title = backstack.get(backstack.size() - 1).getTitle();
                viewHolder.title.setText(title.getDisplayText());
                ViewUtil.loadImageUrlInto(viewHolder.thumbnail, title.getThumbUrl());
            }
            return convertView;
        }
    }
}
