package org.wikipedia.page.gallery;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import static org.wikipedia.util.ShareUtil.buildImageShareChooserIntent;

public final class SavedImageNotificationHelper {

    public static void displayImageSavedNotification(String filename, String fileInfoUrl, Bitmap savedImageBitmap, Uri contentUri) {
        NotificationManager notificationManager = (NotificationManager) WikipediaApp.getInstance()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = buildNotification(filename, fileInfoUrl, savedImageBitmap, contentUri);
        notificationManager.notify(0, notification);
    }

    private static Notification buildNotification(String filename, String fileInfoUrl, Bitmap savedImageBitmap, Uri contentUri) {
        Intent shareChooserIntent = buildImageShareChooserIntent(WikipediaApp.getInstance(),
                filename, fileInfoUrl, contentUri);
        Intent viewImageIntent = buildViewInDefaultAppIntent(contentUri);
        PendingIntent savedImageSharePendingIntent = getPendingIntent(shareChooserIntent);
        PendingIntent viewInDefaultViewerAppPendingIntent = getPendingIntent(viewImageIntent);
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle().bigPicture(savedImageBitmap);
        NotificationCompat.Action savedImageShareAction = buildSavedImageShareAction(savedImageSharePendingIntent);

        return new NotificationCompat.Builder(WikipediaApp.getInstance())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.gallery_image_saved_notification_title))
                .setContentText(getString(R.string.gallery_image_saved_notification_text))
                .setContentIntent(viewInDefaultViewerAppPendingIntent)
                .setStyle(bigPictureStyle)
                .addAction(savedImageShareAction)
                .build();
    }

    private static NotificationCompat.Action buildSavedImageShareAction(PendingIntent pi) {
        return new NotificationCompat.Action.Builder(R.drawable.ic_share_dark,
                WikipediaApp.getInstance().getResources().getString(R.string.gallery_menu_share), pi)
                .build();
    }

    private static PendingIntent getPendingIntent(Intent i) {
        return PendingIntent.getActivity(WikipediaApp.getInstance(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static Intent buildViewInDefaultAppIntent(Uri contentUri) {
        return new Intent().setAction(android.content.Intent.ACTION_VIEW)
                .setDataAndType(contentUri, "image/jpeg");
    }

    private static String getString(int stringId) {
        return WikipediaApp.getInstance().getResources().getString(stringId);
    }

    private SavedImageNotificationHelper() { }
}

