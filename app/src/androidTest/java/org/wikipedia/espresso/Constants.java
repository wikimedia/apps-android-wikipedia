package org.wikipedia.espresso;

import java.util.Locale;

public final class Constants {

    public static final String TEST_OUTPUT_FOLDER = "/wikipedia-app-tests/";
    public static final String TEST_COMPARISON_OUTPUT_FOLDER = "/wikipedia-app-tests/comparison/";
    public static final String TEST_SCREENSHOTS_ASSET_FOLDER = "espresso/screenshots/";
    public static final String TEST_JSON_ASSET_FOLDER = "espresso/json/";
    public static final int SCREENSHOT_COMPRESSION_QUALITY = 100;
    public static final float SCREENSHOT_COMPARE_PERCENT_TOLERANCE = 0f;
    public static final int HEIGHT_OF_TESTING_DEVICE = 1280;
    public static final int WIDTH_OF_TESTING_DEVICE = 720;
    public static final int SDK_VERSION_OF_TESTING_DEVICE = 26;
    public static final String DEFAULT_LANGUAGE_OF_TESTING_DEVICE = Locale.ENGLISH.getDisplayLanguage();

    private Constants() { }
}
