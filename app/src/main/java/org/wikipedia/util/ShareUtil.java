package org.wikipedia.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;

public final class ShareUtil {
    public static final String APP_PACKAGE_REGEX = "org\\.wikipedia.*";
    private static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";
    private static final String FILE_PREFIX = "file://";

    /**
     * Share some text and subject (title) as plain text using an activity chooser,
     * so that the user can choose the app with which to share the content.
     */
    public static void shareText(final Context context, final String subject, final String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.setType("text/plain");
        Intent chooserIntent = createChooserIntent(shareIntent,
                context.getString(R.string.share_via), context);
        if (chooserIntent == null) {
            showUnresolvableIntentMessage(context);
        } else {
            context.startActivity(chooserIntent);
        }
    }

    public static void shareText(final Context context, final PageTitle title) {
        shareText(context, title.getDisplayText(), UriUtil.getUrlWithProvenance(context, title, R.string.prov_share_link));
    }

    /**
     * Share a bitmap image using an activity chooser, so that the user can choose the
     * app with which to share the content.
     * This is done by saving the image to a temporary file in external storage, then specifying
     * that file in the share intent. The name of the temporary file is kept constant, so that
     * it's overwritten every time an image is shared from the app, so that it takes up a
     * constant amount of space.
     */
    public static void shareImage(final Context context, final Bitmap bmp,
                                  final String imageFileName, final String subject,
                                  final String text) {
        new SaneAsyncTask<Uri>() {
            @Override
            public Uri performTask() throws Throwable {
                File processedBitmap = processBitmapForSharing(context, bmp, imageFileName);
                return getUriFromFile(context, processedBitmap);
            }

            @Override
            public void onFinish(Uri result) {
                if (result == null) {
                    displayShareErrorMessage(context);
                    return;
                }
                Intent chooserIntent = buildImageShareChooserIntent(context, subject, text, result);
                context.startActivity(chooserIntent);
            }

            @Override
            public void onCatch(Throwable caught) {
                displayOnCatchMessage(caught, context);
            }
        }.execute();
    }

    public static String getFeaturedImageShareSubject(@NonNull Context context, int age) {
        return context.getString(R.string.feed_featured_image_share_subject) + " | " + DateUtil.getFeedCardDateString(age);
    }

    public static Intent buildImageShareChooserIntent(Context context, String subject, String text, Uri uri) {
        Intent shareIntent = createImageShareIntent(subject, text, uri);
        return Intent.createChooser(shareIntent,
                context.getResources().getString(R.string.share_via));
    }


    public static Uri getUriFromFile(@NonNull Context context, @NonNull File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                : Uri.parse(FILE_PREFIX + file.getAbsolutePath());
    }

    private static File processBitmapForSharing(final Context context, final Bitmap bmp,
                                                final String imageFileName) throws IOException {
        File shareFolder = getClearShareFolder(context);
        if (shareFolder == null) {
            return null;
        }
        shareFolder.mkdirs();
        ByteArrayOutputStream bytes = FileUtil.compressBmpToJpg(bmp);
        return FileUtil.writeToFile(bytes, new File(shareFolder, cleanFileName(imageFileName)));
    }

    private static Intent createImageShareIntent(String subject, String text, Uri uri) {
        return new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType("image/jpeg");
    }

    private static void displayOnCatchMessage(Throwable caught, Context context) {
        Toast.makeText(context,
                String.format(context.getString(R.string.gallery_share_error),
                        caught.getLocalizedMessage()), Toast.LENGTH_SHORT).show();
    }

    private static void displayShareErrorMessage(Context context) {
        Toast.makeText(context,
                String.format(context.getString(R.string.gallery_share_error),
                        context.getString(R.string.err_cannot_save_file)),
                Toast.LENGTH_SHORT).show();
    }

    public static void showUnresolvableIntentMessage(Context context) {
        Toast.makeText(context, R.string.error_can_not_process_link, Toast.LENGTH_LONG).show();
    }

    /**
     * Cleans up and returns the internal cache subdirectory for share-a-fact images.
     */
    public static File getClearShareFolder(Context context) {
        try {
            File dir = new File(getShareFolder(context), "share");
            FileUtil.deleteRecursively(dir);
            return dir;
        } catch (Throwable caught) {
            L.e("Caught " + caught.getMessage(), caught);
        }
        return null;
    }

    public static File getShareFolder(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? context.getCacheDir() : context.getExternalFilesDir(null);
    }

    private static String cleanFileName(String fileName) {
        // Google+ doesn't like file names that have characters %28, %29, %2C
        fileName = fileName.replaceAll("%2[0-9A-F]", "_")
                .replaceAll("[^0-9a-zA-Z-_\\.]", "_")
                .replaceAll("_+", "_");
        // ensure file name ends with .jpg
        if (!fileName.endsWith(".jpg")) {
            fileName = fileName + ".jpg";
        }
        return fileName;
    }

    @Nullable
    public static Intent createChooserIntent(@NonNull Intent targetIntent,
                                             @Nullable CharSequence chooserTitle,
                                             @NonNull Context context) {
        return createChooserIntent(targetIntent, chooserTitle, context, APP_PACKAGE_REGEX);
    }

    @Nullable
    public static Intent createChooserIntent(@NonNull Intent targetIntent,
                                             @Nullable CharSequence chooserTitle,
                                             @NonNull Context context,
                                             String packageNameBlacklistRegex) {
        List<Intent> intents = queryIntents(context, targetIntent, packageNameBlacklistRegex);

        if (intents.isEmpty()) {
            return null;
        }

        Intent bestIntent = Intent.createChooser(intents.remove(0), chooserTitle);
        bestIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
        return bestIntent;
    }

    public static List<Intent> queryIntents(@NonNull Context context,
                                            @NonNull Intent targetIntent,
                                            String packageNameBlacklistRegex) {
        List<Intent> intents = new ArrayList<>();
        Intent queryIntent = new Intent(targetIntent);
        if (targetIntent.getAction().equals(Intent.ACTION_VIEW)) {
            // To avoid using the Wikipedia app externally opens the wikipedia.org links,
            // we can put a non-wikipedia link for intent choose to fetch browser apps list, and use the list for our "true" external links
            queryIntent.setData(Uri.parse("https://not.a.website/"));
        }
        for (ResolveInfo intentActivity : queryIntentActivities(queryIntent, context)) {
            if (!isIntentActivityBlacklisted(intentActivity, packageNameBlacklistRegex)) {
                intents.add(buildLabeledIntent(targetIntent, intentActivity));
            }
        }
        return intents;
    }

    public static List<ResolveInfo> queryIntentActivities(Intent intent, @NonNull Context context) {
        return context.getPackageManager().queryIntentActivities(intent, 0);
    }

    public static boolean canOpenUrlInApp(@NonNull Context context, @NonNull String url) {
        boolean canOpen = false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        for (ResolveInfo intentActivity : queryIntentActivities(intent, context)) {
            if (getPackageName(intentActivity).matches(APP_PACKAGE_REGEX)) {
                canOpen = true;
                break;
            }
        }
        return canOpen;
    }

    private static boolean isIntentActivityBlacklisted(@Nullable ResolveInfo intentActivity,
                                                       @Nullable String packageNameBlacklistRegex) {
        return intentActivity != null
                && getPackageName(intentActivity).matches(defaultString(packageNameBlacklistRegex));
    }

    private static LabeledIntent buildLabeledIntent(Intent intent, ResolveInfo intentActivity) {

        LabeledIntent labeledIntent = new LabeledIntent(intent, intentActivity.resolvePackageName,
                intentActivity.labelRes, intentActivity.getIconResource());
        labeledIntent.setPackage(getPackageName(intentActivity));
        labeledIntent.setClassName(getPackageName(intentActivity), intentActivity.activityInfo.name);
        return labeledIntent;
    }

    private static String getPackageName(@NonNull ResolveInfo intentActivity) {
        return intentActivity.activityInfo.packageName;
    }

    private ShareUtil() { }
}
