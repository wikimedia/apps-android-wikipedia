package org.wikipedia.espresso.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import org.junit.Assert;
import org.wikipedia.util.MathUtil;
import org.wikipedia.util.log.L;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.wikipedia.espresso.Constants.SCREENSHOT_COMPARE_PERCENT_TOLERANCE;
import static org.wikipedia.espresso.Constants.TEST_COMPARISON_OUTPUT_FOLDER;
import static org.wikipedia.espresso.Constants.TEST_OUTPUT_FOLDER;
import static org.wikipedia.espresso.Constants.TEST_SCREENSHOTS_ASSET_FOLDER;

@SuppressWarnings("checkstyle:magicnumber")
public final class CompareTools {

    public static void assertScreenshotWithinTolerance(String screenshotName) throws Exception {
        Assert.assertTrue("Screenshot \"" + screenshotName + "\" difference above tolerance. Please run tests locally and check output image in the "
                        + TEST_COMPARISON_OUTPUT_FOLDER + " folder.",
                CompareTools.compareScreenshotAgainstReference(screenshotName) <= SCREENSHOT_COMPARE_PERCENT_TOLERANCE);
    }

    public static float compareScreenshotAgainstReference(String fileName) throws Exception {
        // source file comes from local folder of device
        InputStream sourceInputStream = new FileInputStream((Environment.getExternalStorageDirectory().getAbsolutePath() + TEST_OUTPUT_FOLDER + fileName + ".png"));
        Bitmap sourceBitmap = BitmapFactory.decodeStream(sourceInputStream);

        // reference file comes from asset folder of the app
        InputStream referenceInputStream = getInstrumentation().getContext().getAssets().open(TEST_SCREENSHOTS_ASSET_FOLDER + fileName + ".png");
        Bitmap referenceBitmap = BitmapFactory.decodeStream(referenceInputStream);

        float compareResult = compareTwoBitmaps(fileName, sourceBitmap, referenceBitmap);
        // TODO: Create a formal tests result instead of output a log
        L.d("Comparison of screenshot \"" + fileName + "\": " + compareResult + "% difference.");

        return compareResult;
    }

    /**
     * Returns the percentage (0 to 100) by which the source bitmap differs from the reference
     * bitmap, i.e. percentage of pixels that are different.
     * The source and reference bitmaps must have the same dimensions.
     */
    private static float compareTwoBitmaps(String comparisonDifferenceFileName, Bitmap source, Bitmap reference) {
        if (source.getWidth() != reference.getWidth()
                || source.getHeight() != reference.getHeight()) {
            throw new RuntimeException("Screenshot " + comparisonDifferenceFileName + " has different dimensions. Is your emulator configuration correct?");
        }

        ByteBuffer sourceBuffer = ByteBuffer.allocate(source.getRowBytes() * source.getHeight());
        source.copyPixelsToBuffer(sourceBuffer);
        byte[] sourceBytes = sourceBuffer.array();

        ByteBuffer referenceBuffer = ByteBuffer.allocate(reference.getRowBytes() * reference.getHeight());
        reference.copyPixelsToBuffer(referenceBuffer);
        byte[] referenceBytes = referenceBuffer.array();

        ByteBuffer resultBuffer = ByteBuffer.allocate(reference.getRowBytes() * reference.getHeight());
        byte[] resultBytes = resultBuffer.array();

        int diffPixels = 0;
        int totalPixels = sourceBytes.length / 4;

        for (int i = 0; i < sourceBytes.length; i += 4) {
            if (sourceBytes[i] != referenceBytes[i]
                    || sourceBytes[i + 1] != referenceBytes[i + 1]
                    || sourceBytes[i + 2] != referenceBytes[i + 2]
                    || sourceBytes[i + 3] != referenceBytes[i + 3]) {
                diffPixels++;
                // The pixel differs, so mark it as Red in the result.
                resultBytes[i] = (byte)0xff;
                resultBytes[i + 1] = 0;
                resultBytes[i + 2] = 0;
                resultBytes[i + 3] = (byte)0xff;
            } else {
                // Copy the pixel to the result.
                resultBytes[i] = sourceBytes[i];
                resultBytes[i + 1] = sourceBytes[i + 1];
                resultBytes[i + 2] = sourceBytes[i + 2];
                resultBytes[i + 3] = sourceBytes[i + 3];
            }
        }

        Bitmap resultBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        resultBitmap.copyPixelsFromBuffer(resultBuffer);

        // TODO: Open it if we decide to use the files in a report. (e.g.: HTML report)
        if (diffPixels > 0) {
            ScreenshotTools.saveImageIntoDisk(TEST_COMPARISON_OUTPUT_FOLDER, comparisonDifferenceFileName, resultBitmap);
        }
        return MathUtil.percentage(diffPixels, totalPixels);
    }

    private CompareTools() { }
}
