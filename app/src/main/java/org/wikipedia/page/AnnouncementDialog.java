package org.wikipedia.page;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.feed.announcement.AnnouncementCard;
import org.wikipedia.feed.announcement.AnnouncementCardView;
import org.wikipedia.feed.configure.ConfigureActivity;
import org.wikipedia.feed.model.Card;
import org.wikipedia.language.LanguageSettingsInvokeSource;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.UriUtil;

public class AnnouncementDialog extends BottomSheetDialog implements AnnouncementCardView.Callback {
    AnnouncementDialog(@NonNull Context context, @NonNull AnnouncementCard card) {
        super(context);
        AnnouncementCardView rootView = new AnnouncementCardView(context);
        rootView.setCard(card);
        rootView.setCallback(this);
        setContentView(rootView);

        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) rootView.getParent());
        behavior.setPeekHeight(DimenUtil.getDisplayHeightPx() / 2);
    }

    @Override
    public void onAnnouncementPositiveAction(@NonNull Card card, @NonNull Uri uri) {
        if (uri.toString().equals(UriUtil.LOCAL_URL_LOGIN)) {
            getContext().startActivity(LoginActivity.newIntent(getContext(), LoginFunnel.SOURCE_NAV));
        } else if (uri.toString().equals(UriUtil.LOCAL_URL_SETTINGS)) {
            getContext().startActivity(SettingsActivity.newIntent(getContext()));
        } else if (uri.toString().equals(UriUtil.LOCAL_URL_CUSTOMIZE_FEED)) {
            getContext().startActivity(ConfigureActivity.newIntent(getContext(), card.type().code()));
        } else if (uri.toString().equals(UriUtil.LOCAL_URL_LANGUAGES)) {
            getContext().startActivity(WikipediaLanguagesActivity.newIntent(getContext(),
                    LanguageSettingsInvokeSource.ANNOUNCEMENT.text()));
        } else {
            UriUtil.handleExternalLink(getContext(), uri);
        }
    }

    @Override
    public void onAnnouncementNegativeAction(@NonNull Card card) {
        dismiss();
    }
}
