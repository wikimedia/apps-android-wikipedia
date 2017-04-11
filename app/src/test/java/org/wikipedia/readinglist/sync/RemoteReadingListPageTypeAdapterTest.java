package org.wikipedia.readinglist.sync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.test.TestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.readinglist.sync.RemoteReadingLists.RemoteReadingList;

@RunWith(TestRunner.class)
public class RemoteReadingListPageTypeAdapterTest {

    private ReadingList localList;
    private List<PageTitle> localTitles;

    @Before
    public void setUp() throws Throwable {

        localTitles = new ArrayList<>();
        localTitles.add(new PageTitle("Barack Obama", new WikiSite("en.wikipedia.org")));
        localTitles.add(new PageTitle("Special:Random", new WikiSite("en.wikipedia.org")));
        localTitles.add(new PageTitle("Файл:Файл какой-то", new WikiSite("ru.wikipedia.org")));

        String listName = "My list";
        long now = System.currentTimeMillis();
        localList = ReadingList.builder()
                .atime(now)
                .mtime(now)
                .title("My list")
                .key(ReadingListDaoProxy.listKey(listName))
                .pages(Collections.<ReadingListPage>emptyList())
                .build();

        for (PageTitle title : localTitles) {
            localList.getPages().add(ReadingListDaoProxy.page(localList, title));
        }
    }

    @Test public void testRemoteReadingListPageTypeAdapter() throws Throwable {

        String serialized = GsonMarshaller.marshal(new RemoteReadingList(localList));

        RemoteReadingList remoteList = GsonUnmarshaller.unmarshal(RemoteReadingList.class, serialized);

        for (int i = 0; i < remoteList.pages().size(); i++) {
            assertThat(remoteList.pages().get(i).lang(), is(localTitles.get(i).getWikiSite().languageCode()));
            assertThat(remoteList.pages().get(i).namespace(), is(localTitles.get(i).namespace().code()));
            assertThat(remoteList.pages().get(i).title(), is(localTitles.get(i).getDisplayText()));
        }
    }
}
