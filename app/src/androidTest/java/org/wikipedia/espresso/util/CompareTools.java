package org.wikipedia.espresso.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.log.L;

import java.io.FileInputStream;
import java.io.InputStream;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.wikipedia.espresso.Constants.WIKIPEDIA_APP_TEST_ASSET_FOLDER;
import static org.wikipedia.espresso.Constants.WIKIPEDIA_APP_TEST_COMPARE_NUMBER;
import static org.wikipedia.espresso.Constants.WIKIPEDIA_APP_TEST_FOLDER;

@RunWith(AndroidJUnit4.class)
public final class CompareTools {

    public static int compare(String fileName) {

        try {
            // source file comes from local folder of device
            InputStream sourceInputStream = new FileInputStream((Environment.getExternalStorageDirectory().getAbsolutePath() + WIKIPEDIA_APP_TEST_FOLDER + fileName + ".png"));
            Bitmap sourceBitmap = BitmapFactory.decodeStream(sourceInputStream);

            // reference file comes from asset folder of the app
            InputStream referenceInputStream = getInstrumentation().getContext().getAssets().open(WIKIPEDIA_APP_TEST_ASSET_FOLDER + fileName + ".png");
            Bitmap referenceBitmap = BitmapFactory.decodeStream(referenceInputStream);

            int compareResult = compareTwoBitmaps("COMPARISON_OF_" + fileName, sourceBitmap, referenceBitmap);
            // TODO: Create a formal tests result instead of output a log
            L.d("===== Compare Result ===== ");
            L.d("Compare page [" + fileName + "] => Match Percentage => " + compareResult + "%");

            return compareResult;

        } catch (Exception e) {
            L.d("Compare Error: " + e);
        }

        return -1;
    }

    private static int compareTwoBitmaps(String fileName, Bitmap source, Bitmap reference) {
        if (source.getWidth() != reference.getWidth()
                || source.getHeight() != reference.getHeight()) {
            return -1;
        }

        int totalCount = 0;
        int diffCount = 0;
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        Bitmap compareResult = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < sourceWidth; x++) {
            for (int y = 0; y < sourceHeight; y++) {

                totalCount++;

                int pixelA = source.getPixel(x, y);
                int pixelB = reference.getPixel(x, y);

                int aA = Color.alpha(pixelA);
                int aR = Color.red(pixelA);
                int aG = Color.green(pixelA);
                int aB = Color.blue(pixelA);

                int bR = Color.red(pixelB);
                int bG = Color.green(pixelB);
                int bB = Color.blue(pixelB);

                if (aR != bR || aG != bG || aB != bB) {
                    diffCount++;
                    compareResult.setPixel(x, y, Color.argb(aA, aR, aG, aB));
                }

            }
        }

        ScreenshotTools.saveImageIntoDisk(fileName, compareResult);

        int percentage = WIKIPEDIA_APP_TEST_COMPARE_NUMBER - MathUtil.percentage(diffCount, totalCount);

        return  (diffCount == 0) ? WIKIPEDIA_APP_TEST_COMPARE_NUMBER : percentage;
    }

    private CompareTools() { }
}
