package org.wikipedia.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.wikipedia.R;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.PageTitle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.StringUtil.emptyIfNull;

public final class ShareUtils {
    public static final String APP_PACKAGE_REGEX = "org\\.wikipedia.*";

    /** Private constructor, so nobody can construct ShareUtils. */
    private ShareUtils() { }

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
        shareText(context, title.getDisplayText(), title.getCanonicalUri());
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
                                  final String text, final boolean recycleBmp) {
        final int jpegQuality = 85;
        new SaneAsyncTask<String>(SaneAsyncTask.SINGLE_THREAD) {
            @Override
            public String performTask() throws Throwable {
                File dir = clearFolder(context);
                if (dir == null) {
                    return null;
                }

                dir.mkdirs();

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, bytes);
                if (recycleBmp) {
                    bmp.recycle();
                }
                File f = new File(dir, cleanFileName(imageFileName));
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();
                return f.getAbsolutePath();
            }

            @Override
            public void onFinish(String result) {
                if (result == null) {
                    Toast.makeText(context,
                            String.format(context.getString(R.string.gallery_share_error),
                                    context.getString(R.string.err_cannot_save_file)),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + result));
                shareIntent.setType("image/jpeg");
                Intent chooserIntent = Intent.createChooser(shareIntent,
                        context.getResources().getString(R.string.share_via));
                context.startActivity(chooserIntent);
            }

            @Override
            public void onCatch(Throwable caught) {
                Toast.makeText(context,
                        String.format(context.getString(R.string.gallery_share_error),
                                caught.getLocalizedMessage()), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public static void showUnresolvableIntentMessage(Context context) {
        Toast.makeText(context, R.string.error_can_not_process_link, Toast.LENGTH_LONG).show();
    }

    /**
     * Cleans up the directory that contains the shared image.
     */
    public static File clearFolder(Context context) {
        if (!isExternalStorageWritable()) {
            return null;
        }

        try {
            File dir = new File(context.getExternalCacheDir(), "img");
            if (dir.isDirectory()) {
                for (String file : dir.list()) {
                    new File(dir, file).delete();
                }
            }
            return dir;
        } catch (Exception e) {
            // There have been a few reports of exceptions coming from the getExternalCacheDir()
            // function, even though we check that external storage is mounted properly.
            // Until we can reproduce it, at least we won't crash.
            Log.d("clearFolder", "Failed to clear shared image folder.", e);
        }
        return null;
    }

    private static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
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
        for (ResolveInfo intentActivity : queryIntentActivities(targetIntent, context)) {
            if (!isIntentActivityBlacklisted(intentActivity, packageNameBlacklistRegex)) {
                intents.add(buildLabeledIntent(targetIntent, intentActivity));
            }
        }
        return intents;
    }

    public static List<ResolveInfo> queryIntentActivities(Intent intent, @NonNull Context context) {
        return context.getPackageManager().queryIntentActivities(intent, 0);
    }

    private static boolean isIntentActivityBlacklisted(@Nullable ResolveInfo intentActivity,
                                                       @Nullable String packageNameBlacklistRegex) {
        return intentActivity != null
                && getPackageName(intentActivity).matches(emptyIfNull(packageNameBlacklistRegex));
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
}
