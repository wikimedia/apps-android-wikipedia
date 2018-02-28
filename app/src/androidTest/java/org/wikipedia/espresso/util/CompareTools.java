package org.wikipedia.espresso.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.log.L;

import java.io.FileInputStream;
import java.io.InputStream;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.wikipedia.espresso.Constants.SCREENSHOT_COMPARE_PERCENT_TOLERANCE;
import static org.wikipedia.espresso.Constants.TEST_ASSET_FOLDER;
import static org.wikipedia.espresso.Constants.TEST_OUTPUT_FOLDER;

@RunWith(AndroidJUnit4.class)
public final class CompareTools {

    public static void assertScreenshotWithinTolerance(String screenshotName) throws Exception {
        Assert.assertTrue("Screenshot " + screenshotName + " difference above tolerance.",
                CompareTools.compareScreenshotAgainstReference(screenshotName) <= SCREENSHOT_COMPARE_PERCENT_TOLERANCE);
    }

    public static int compareScreenshotAgainstReference(String fileName) throws Exception {
        // source file comes from local folder of device
        InputStream sourceInputStream = new FileInputStream((Environment.getExternalStorageDirectory().getAbsolutePath() + TEST_OUTPUT_FOLDER + fileName + ".png"));
        Bitmap sourceBitmap = BitmapFactory.decodeStream(sourceInputStream);

        // reference file comes from asset folder of the app
        InputStream referenceInputStream = getInstrumentation().getContext().getAssets().open(TEST_ASSET_FOLDER + fileName + ".png");
        Bitmap referenceBitmap = BitmapFactory.decodeStream(referenceInputStream);

        int compareResult = compareTwoBitmaps("COMPARISON_OF_" + fileName, sourceBitmap, referenceBitmap);
        // TODO: Create a formal tests result instead of output a log
        L.d("===== Compare Result ===== ");
        L.d("Compare page [" + fileName + "] => Match Percentage => " + compareResult + "%");

        return compareResult;
    }

    /**
     * Returns the percentage (0 to 100) by which the source bitmap differs from the reference
     * bitmap, i.e. percentage of pixels that are different.
     * The source and reference bitmaps must have the same dimensions.
     */
    private static int compareTwoBitmaps(String comparisonDifferenceFileName, Bitmap source, Bitmap reference) {
        if (source.getWidth() != reference.getWidth()
                || source.getHeight() != reference.getHeight()) {
            throw new RuntimeException("Screenshot " + comparisonDifferenceFileName + " has different dimensions. Is your emulator configuration correct?");
        }

        int totalPixels = 0;
        int diffPixels = 0;
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        Bitmap compareResult = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < sourceWidth; x++) {
            for (int y = 0; y < sourceHeight; y++) {

                totalPixels++;

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
                    diffPixels++;
                    compareResult.setPixel(x, y, Color.argb(aA, aR, aG, aB));
                }
            }
        }

        // TODO: Open it if we decide to use the files in a report. (e.g.: HTML report)
        if (diffPixels > 0) {
            ScreenshotTools.saveImageIntoDisk(comparisonDifferenceFileName, compareResult);
        }
        return MathUtil.percentage(diffPixels, totalPixels);
    }

    private CompareTools() { }
}
