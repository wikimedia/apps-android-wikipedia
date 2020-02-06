package org.wikipedia.util;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.json.GsonUnmarshaller;

import java.io.IOException;

import static org.wikipedia.util.SavedPagesConversionUtil.CONVERTED_FILES_DIRECTORY_NAME;
import static org.wikipedia.util.SavedPagesConversionUtil.PAGE_SUMMARY_DIRECTORY_NAME;

public final class PageSummaryConstructionHelperUtil {
    private static final String NAMESPACE_ID = "\"@@NS_ID@@\"";
    private static final String WIKIBASE_ITEM = "@@WIKIBASE_ITEM@@";
    private static final String PAGE_ID = "\"@@PAGE_ID@@\"";
    private static final String DISPLAY_TITLE = "@@DISPLAYTITLE@@";
    private static final String CANONICAL_TITLE = "@@CANONICAL@@";
    private static final String REVISOIN = "@@REVISION@@";
    private static final String TIMESTAMP = "@@TIMESTAMP@@";
    private static final String DESCRIPTION = "@@DESCRIPTION@@";
    private static final String DESCRIPTION_SOURCE = "@@DESCRIPTION_SOURCE@@";
    private static final String LAT = "@@LAT@@";
    private static final String LON = "@@LON@@";

    public static void constructAndStorePageSummaryFrom(String leadJSON, String title) {
        PageLead pageLead = GsonUnmarshaller.unmarshal(PageLead.class, leadJSON);
        try {
            if (pageLead != null) {
                String json = FileUtil.readFile(WikipediaApp.getInstance().getAssets().open("page_summary.json")).
                        replace(NAMESPACE_ID, "" + pageLead.getNamespace().code()).
                        replace(WIKIBASE_ITEM, pageLead.getWikiBaseItem()).replaceAll(DISPLAY_TITLE, pageLead.getDisplayTitle()).
                        replace(PAGE_ID, "" + pageLead.getId()).replace(REVISOIN, "" + pageLead.getRevision()).
                        replaceAll(CANONICAL_TITLE, pageLead.getNormalizedTitle()).replaceAll(TIMESTAMP, pageLead.getLastModified()).
                        replace(DESCRIPTION, pageLead.getDescription()).replace(DESCRIPTION_SOURCE, pageLead.getDescriptionSource())
                        .replace(LAT, "" + (pageLead.getGeo() == null ? "" : pageLead.getGeo().getLatitude()))
                        .replace(LON, "" + (pageLead.getGeo() == null ? "" : pageLead.getGeo().getLongitude()));
                FileUtil.writeToFileInDirectory(json, WikipediaApp.getInstance().getFilesDir() + "/" + CONVERTED_FILES_DIRECTORY_NAME + PAGE_SUMMARY_DIRECTORY_NAME, title);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private PageSummaryConstructionHelperUtil() {
    }
}
