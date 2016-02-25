package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.Site;
import org.wikipedia.util.log.L;

import java.util.Arrays;
import java.util.List;

public class PageLoadTest extends BasePageLoadTest {
    @Test
    public void testPageFetch() throws Throwable {
//        final int expectedNumberOfSections = 4;
        // TODO: verify num sections
        loadPageSync("Test_page_for_app_testing/Section1");
    }

    /** Inspired by https://bugzilla.wikimedia.org/show_bug.cgi?id=66152 */
    @Test
    public void testPageFetchWithAmpersand() throws Throwable {
//        final int expectedNumberOfSections = 1;
        // TODO: verify num sections
        loadPageSync("Ampersand & title", TEST_SITE);
    }

    @Test
    public void testPagesWithImagesLandscape() throws Throwable {
        requestLandscapeOrientation();
        testPagesWithImages();
    }

    @Test
    public void testPagesWithImagesPortrait() throws Throwable {
        requestPortraitOrientation();
        testPagesWithImages();
    }

    @Test
    public void testPagesWithoutImagesLandscape() throws Throwable {
        requestLandscapeOrientation();
        testPagesWithoutImages();
    }

    @Test
    public void testPagesWithoutImagesPortrait() throws Throwable {
        requestPortraitOrientation();
        testPagesWithoutImages();
    }

    @Test
    public void testPagesRtlLandscape() throws Throwable {
        requestLandscapeOrientation();
        testPagesRtl();
    }

    @Test
    public void testPagesRtlPortrait() throws Throwable {
        requestPortraitOrientation();
        testPagesRtl();
    }

    private void testPagesWithImages() throws Throwable {
        List<String> pages = Arrays.asList("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn",
                "Uranus", "Neptune", "Barack Obama", "Moon");
        testPages(pages);
    }

    private void testPagesWithoutImages() throws Throwable {
        // From https://en.wikipedia.org/wiki/Mecyclothorax
        List<String> pages = Arrays.asList("Mecyclothorax aa", "Mecyclothorax abax",
                "Mecyclothorax acutangulus", "Mecyclothorax aeneipennis", "Mecyclothorax aeneus",
                "Mecyclothorax altiusculoides", "Mecyclothorax altiusculus",
                "Mecyclothorax amaroides", "Mecyclothorax ambiguus", "Mecyclothorax angulosus");
        testPages(pages);
    }

    private void testPagesRtl() throws Throwable {
        List<String> pages = Arrays.asList("Algebra", "Water", "Dinosaur", "Helium");
        testPages(pages, Site.forLanguageCode("ar"));
    }

    private void testPages(@NonNull List<String> titles) throws Throwable {
        testPages(titles, EN_SITE);
    }

    private void testPages(@NonNull List<String> titles, @NonNull Site site) throws Throwable {
        for (String title : titles) {
            String tag = titleToTag(title);
            L.d("title=" + title + " tag=" + tag);
            loadPageSync(title, site);
            screenshot(tag);

            // TODO: this is nondeterministic. Add listeners for load and Ken Burns animation, where
            // applicable, completion.
            final int millis = 5000;
            Thread.sleep(millis);
        }
    }

    private String titleToTag(@NonNull String title) {
        String illegalChars = "[^a-zA-Z0-9_-]";
        return title.replaceAll(illegalChars, "_");
    }
}
