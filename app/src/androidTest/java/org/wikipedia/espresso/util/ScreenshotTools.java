package org.wikipedia.espresso.util;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import org.hamcrest.Matcher;
import org.wikipedia.util.log.L;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static org.wikipedia.espresso.Constants.SCREENSHOT_COMPRESSION_QUALITY;
import static org.wikipedia.espresso.Constants.TEST_OUTPUT_FOLDER;

public final class ScreenshotTools {

    public static void takeScreenshot(String fileName, View view) {
        View scrView = view.getRootView();
        scrView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(scrView.getDrawingCache());
        scrView.setDrawingCacheEnabled(false);

        saveImageIntoDisk(fileName, bitmap);
    }

    public static void saveImageIntoDisk(String fileName, Bitmap bitmap) {
        saveImageIntoDisk(TEST_OUTPUT_FOLDER, fileName, bitmap);
    }

    public static void saveImageIntoDisk(String folderName, String fileName, Bitmap bitmap) {

        File folder = new File(Environment.getExternalStorageDirectory() + folderName);
        if (!folder.exists()) {
            folder.mkdir();
        }

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + folderName + fileName + ".png";

        OutputStream out = null;
        File imageFile = new File(path);

        try {
            out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, SCREENSHOT_COMPRESSION_QUALITY, out);
            out.flush();
        } catch (FileNotFoundException e) {
            L.d("File Error FileNotFoundException => " + e);
        } catch (IOException e) {
            L.d("File Error IOException => " + e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception e) {

                L.d("File Error Exception => " + e);
            }

        }
    }


    public static ViewAction viewSnap(final String fileName) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Do screen rotation";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                takeScreenshot(fileName, view);
            }
        };
    }

    public static void snap(final String fileName) {
        onView(isRoot()).perform(viewSnap(fileName));
    }


    private ScreenshotTools() { }
}
