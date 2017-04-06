package org.wikipedia.json;


import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.tabs.Tab;

import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

// PageTitle.PageProperties.Location uses a Bundle which is incompatible with Robolectric
public class TabUnmarshallerTest {
    @Test public void testUnmarshalNull() {
        assertThat(TabUnmarshaller.unmarshal(null), empty());
    }

    @Test public void testUnmarshalSingle() {
        PageTitle page = new PageTitle("text", WikiSite.forLanguageCode("test"));
        HistoryEntry history = new HistoryEntry(page, new Date(0), HistoryEntry.SOURCE_SEARCH);
        Tab tab = new Tab();
        tab.getBackStack().add(new PageBackStackItem(page, history));
        List<Tab> tabs = singletonList(tab);

        assertThat(TabUnmarshaller.unmarshal(GsonMarshaller.marshal(tabs)), is(tabs));
    }

    // T152980
    @Test(expected = RuntimeException.class) public void testUnmarshalNoPageTitleAuthority() {
        PageTitle page = new PageTitle("text", new WikiSite("", ""));
        HistoryEntry history = new HistoryEntry(page, new Date(0), HistoryEntry.SOURCE_SEARCH);
        Tab tab = new Tab();
        tab.getBackStack().add(new PageBackStackItem(page, history));
        List<Tab> tabs = singletonList(tab);

        TabUnmarshaller.unmarshal(GsonMarshaller.marshal(tabs));
    }

    // T152980
    @Test(expected = RuntimeException.class) public void testUnmarshalNoHistoryEntryAuthority() {
        PageTitle page = new PageTitle("text", WikiSite.forLanguageCode("test"));
        PageTitle prevPage = new PageTitle("text", new WikiSite("", ""));
        HistoryEntry history = new HistoryEntry(prevPage, new Date(0), HistoryEntry.SOURCE_SEARCH);
        Tab tab = new Tab();
        tab.getBackStack().add(new PageBackStackItem(page, history));
        List<Tab> tabs = singletonList(tab);

        TabUnmarshaller.unmarshal(GsonMarshaller.marshal(tabs));
    }
}
