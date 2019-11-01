package org.wikipedia.page;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.feed.announcement.AnnouncementCard;
import org.wikipedia.feed.announcement.AnnouncementCardView;
import org.wikipedia.feed.configure.ConfigureActivity;
import org.wikipedia.feed.model.Card;
import org.wikipedia.language.LanguageSettingsInvokeSource;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.util.UriUtil;

import java.util.Calendar;

public class AnnouncementDialog extends AlertDialog implements AnnouncementCardView.Callback {
    AnnouncementDialog(@NonNull Context context, @NonNull AnnouncementCard card) {
        super(context);
        AnnouncementCardView rootView = new AnnouncementCardView(context);
        rootView.setCard(card);
        rootView.setCallback(this);
        setView(rootView);
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
        Prefs.setFundraisingDialogShownInYear(Calendar.getInstance().get(Calendar.YEAR));
        dismiss();
    }

    @Override
    public void onAnnouncementNegativeAction(@NonNull Card card) {
        dismiss();
        Prefs.setFundraisingDialogShownInYear(Calendar.getInstance().get(Calendar.YEAR));
    }
}
