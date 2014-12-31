package org.wikipedia.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import org.wikipedia.R;
import org.wikipedia.concurrency.SaneAsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 *
 */
public final class ShareUtils {
    /** Private constructor, so nobody can construct ShareUtils. */
    private ShareUtils() { }

    /**
     * Share a bitmap image using an activity chooser, so that the user can choose the
     * app with which to share the content.
     * This is done by saving the image to a temporary file in external storage, then specifying
     * that file in the share intent. The name of the temporary file is kept constant, so that
     * it's overwritten every time an image is shared from the app, so that it takes up a
     * constant amount of space.
     */
    public static void shareImage(final Activity activity, final Bitmap bmp, final String mimeType,
                                  final String subject, final String text, final boolean recycleBmp) {
        final int jpegQuality = 85;
        final String tempShareFileName = activity.getPackageName() + "_tempShareImage.jpg";
        new SaneAsyncTask<String>(SaneAsyncTask.SINGLE_THREAD) {
            @Override
            public String performTask() throws Throwable {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, bytes);
                if (recycleBmp) {
                    bmp.recycle();
                }
                File f = new File(Environment.getExternalStorageDirectory() + File.separator
                        + tempShareFileName);
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();
                return f.getAbsolutePath();
            }

            @Override
            public void onFinish(String result) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + result));
                shareIntent.setType(mimeType);
                Intent chooser = Intent.createChooser(shareIntent,
                        activity.getResources().getString(R.string.share_via));
                activity.startActivity(chooser);
            }

            @Override
            public void onCatch(Throwable caught) {
                Toast.makeText(activity,
                        String.format(activity.getString(R.string.gallery_share_error),
                                caught.getLocalizedMessage()), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }
}
