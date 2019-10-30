package org.wikipedia.page;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.wikipedia.feed.announcement.AnnouncementCard;
import org.wikipedia.feed.announcement.AnnouncementCardView;

public class AnnouncementDialog extends BottomSheetDialog {
    AnnouncementDialog(@NonNull Context context, @NonNull AnnouncementCard card) {
        super(context);
        AnnouncementCardView rootView = new AnnouncementCardView(context);
        rootView.setCard(card);
        setContentView(rootView);

        // TODO: adjust peek height
    }
}
