package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.settings.Prefs;

public class ContinueReadingClient {
    public void request(@NonNull Context context, @NonNull LastPageReadTask.Callback cb) {
        new LastPageReadTask(context, cb).execute();
    }

    @Nullable public String lastDismissedTitle() {
        return Prefs.dismissedContinueReadingTitle();
    }

    // todo: [Feed] dismissal of the Continue Reading card should invoke this method.
    public void dismissContinueReadingTitle(@Nullable String title) {
        Prefs.dismissContinueReadingTitle(title);
    }
}