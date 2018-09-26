package org.wikipedia.notifications;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.NotificationFunnel;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class NotificationItemActionsDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void onArchive(@NonNull Notification notification);
        void onActionPageTitle(@NonNull PageTitle pageTitle);
        boolean isShowingArchived();
    }

    @BindView(R.id.notification_item_text) TextView titleView;
    @BindView(R.id.notification_item_archive_icon) ImageView archiveIconView;
    @BindView(R.id.notification_item_archive_text) TextView archiveTextView;
    @BindView(R.id.notification_action_primary) View primaryView;
    @BindView(R.id.notification_action_primary_icon) AppCompatImageView primaryImageView;
    @BindView(R.id.notification_action_primary_text) TextView primaryTextView;
    @BindView(R.id.notification_action_secondary) View secondaryView;
    @BindView(R.id.notification_action_secondary_icon) AppCompatImageView secondaryImageView;
    @BindView(R.id.notification_action_secondary_text) TextView secondaryTextView;
    @BindView(R.id.notification_action_tertiary) View tertiaryView;
    @BindView(R.id.notification_action_tertiary_icon) AppCompatImageView tertiaryImageView;
    @BindView(R.id.notification_action_tertiary_text) TextView tertiaryTextView;
    private Unbinder unbinder;

    private Notification notification;
    private NotificationLinkHandler linkHandler;

    @NonNull
    public static NotificationItemActionsDialog newInstance(@NonNull Notification notification) {
        NotificationItemActionsDialog instance = new NotificationItemActionsDialog();
        Bundle args = new Bundle();
        args.putString("notification", GsonUtil.getDefaultGson().toJson(notification));
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_notification_actions, container);
        unbinder = ButterKnife.bind(this, view);
        notification = GsonUtil.getDefaultGson().fromJson(getArguments().getString("notification"), Notification.class);
        linkHandler = new NotificationLinkHandler(requireContext());

        if (notification.getContents() != null) {
            titleView.setText(StringUtil.fromHtml(notification.getContents().getHeader()).toString());
        }

        if (notification.getContents() != null && notification.getContents().getLinks() != null
                && notification.getContents().getLinks().getPrimary() != null) {
            setUpViewForLink(primaryView, primaryImageView, primaryTextView, notification.getContents().getLinks().getPrimary());
        } else {
            primaryView.setVisibility(View.GONE);
        }

        if (notification.getContents() != null && notification.getContents().getLinks() != null
                && notification.getContents().getLinks().getSecondary() != null && notification.getContents().getLinks().getSecondary().size() > 0) {
            setUpViewForLink(secondaryView, secondaryImageView, secondaryTextView, notification.getContents().getLinks().getSecondary().get(0));
        } else {
            secondaryView.setVisibility(View.GONE);
        }

        if (notification.getContents() != null && notification.getContents().getLinks() != null
                && notification.getContents().getLinks().getSecondary() != null && notification.getContents().getLinks().getSecondary().size() > 1) {
            setUpViewForLink(tertiaryView, tertiaryImageView, tertiaryTextView, notification.getContents().getLinks().getSecondary().get(1));
        } else {
            tertiaryView.setVisibility(View.GONE);
        }

        archiveIconView.setImageResource(callback().isShowingArchived() ? R.drawable.ic_unarchive_themed_24dp : R.drawable.ic_archive_themed_24dp);
        archiveTextView.setText(callback().isShowingArchived() ? R.string.notifications_mark_unread : R.string.notifications_archive);

        return view;
    }

    @Override
    public void onDestroyView() {
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
        super.onDestroyView();
    }

    @OnClick(R.id.notification_item_archive) void onArchiveClick(View v) {
        callback().onArchive(notification);
    }

    @OnClick({R.id.notification_action_primary,
            R.id.notification_action_secondary,
            R.id.notification_action_tertiary})
    void onActionClick(View v) {
        Notification.Link link = (Notification.Link) v.getTag();
        int linkIndex = v.getId() == R.id.notification_action_primary ? 0 : v.getId() == R.id.notification_action_secondary ? 1 : 2;
        String url = link.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        new NotificationFunnel(WikipediaApp.getInstance(), notification).logAction(linkIndex, link);
        linkHandler.setWikiSite(new WikiSite(url));
        linkHandler.onUrlClick(url, null, "");
    }

    private void setUpViewForLink(View containerView, AppCompatImageView iconView, TextView labelView, @NonNull Notification.Link link) {
        if (!TextUtils.isEmpty(link.getTooltip())) {
            labelView.setText(StringUtil.fromHtml(link.getTooltip()));
        } else {
            labelView.setText(StringUtil.fromHtml(link.getLabel()));
        }
        if ("userAvatar".equals(link.getIcon())) {
            iconView.setImageResource(R.drawable.ic_user_avatar);
        } else {
            iconView.setImageResource(R.drawable.ic_arrow_forward_black_24dp);
        }
        containerView.setTag(link);
        containerView.setVisibility(View.VISIBLE);
    }

    private class NotificationLinkHandler extends LinkHandler {
        private WikiSite wikiSite;

        NotificationLinkHandler(@NonNull Context context) {
            super(context);
        }

        public void setWikiSite(@NonNull WikiSite wikiSite) {
            this.wikiSite = wikiSite;
        }

        @Override
        public void onPageLinkClicked(@NonNull String anchor, @NonNull String linkText) {
            // ignore
        }

        @Override
        public void onInternalLinkClicked(@NonNull PageTitle title) {
            callback().onActionPageTitle(title);
        }

        @Override
        public void onExternalLinkClicked(@NonNull Uri uri) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
            } catch (Exception e) {
                L.e(e);
            }
        }

        @Override
        public WikiSite getWikiSite() {
            return wikiSite;
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
