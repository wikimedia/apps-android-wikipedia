package org.wikipedia.readinglist.database;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = WikipediaApp.class)
public class ReadingListDbHelperTest {
    private ReadingListDbHelper readingListDbHelper;

    @Before
    public void setup() {
        readingListDbHelper = ReadingListDbHelper.instance();
    }

    @Test
    public void testGetAllListsForFullList() {
        List<ReadingList> lists = readingListDbHelper.getAllLists();
        int initialSize = lists.size();
        ReadingList list = readingListDbHelper.createList("Test", "TestDesc");
        Assert.assertEquals(initialSize + 1, readingListDbHelper.getAllLists().size());
        readingListDbHelper.deleteList(list);
        Assert.assertEquals(initialSize, readingListDbHelper.getAllLists().size());
    }

    @Test
    public void testGetAllListsWithoutContentsForSize() {
        List<ReadingList> lists = readingListDbHelper.getAllListsWithoutContents();
        boolean isEmpty = true;
        for (ReadingList list : lists) {
            if (list.pages().size() > 0) {
                isEmpty = false;
            }
        }
        Assert.assertTrue(isEmpty);
    }

    @Test
    public void testCreateListForNewList() {
        List<ReadingList> lists = readingListDbHelper.getAllLists();
        int initialSize = lists.size();
        ReadingList list = readingListDbHelper.createList("Test1", "TestDesc");
        Assert.assertEquals(initialSize + 1, readingListDbHelper.getAllLists().size());
        readingListDbHelper.deleteList(list);
    }

    @Test
    public void testUpdateListForFieldUpdate() {
        ReadingList list = readingListDbHelper.createList("Test2", "TestDesc");
        list.title("testTitle");
        readingListDbHelper.updateList(list, false);
        List<ReadingList> lists = readingListDbHelper.getAllLists();
        Assert.assertTrue(hasTitle(lists, "testTitle"));
        Assert.assertFalse(hasTitle(lists, "Test2"));
        readingListDbHelper.deleteList(list);
    }

    @Test
    public void testDeleteListForDeletion() {
        List<ReadingList> lists = readingListDbHelper.getAllLists();
        int initialSize = lists.size();
        ReadingList list = readingListDbHelper.createList("Test3", "TestDesc");
        readingListDbHelper.deleteList(list);
        Assert.assertEquals(initialSize, readingListDbHelper.getAllLists().size());
        Assert.assertFalse(hasTitle(lists, "Test3"));
    }

    @Test
    public void testAddPageToListForPageAddition() {
        PageTitle page = new PageTitle("1", WikipediaApp.getInstance().getWikiSite());
        ReadingList list = readingListDbHelper.createList("Test4", "TestDesc");
        ReadingList list2 = readingListDbHelper.createList("Test5", "TestDesc");
        readingListDbHelper.addPageToList(list, page, false);
        readingListDbHelper.addPageToList(list2, page, false);
        List<ReadingListPage> pages = new ArrayList<>();
        pages.add(new ReadingListPage(page));
        List<ReadingList> lists = readingListDbHelper.getListsFromPageOccurrences(pages);
        readingListDbHelper.deleteList(list);
        readingListDbHelper.deleteList(list2);
        readingListDbHelper.markPagesForDeletion(list, pages);
        readingListDbHelper.markPagesForDeletion(list2, pages);
        Assert.assertFalse(lists.contains(list));
    }

    public void testGetAllPagesToBeSavedForRetrievingPages() {
        PageTitle page = new PageTitle("1", WikipediaApp.getInstance().getWikiSite());
        ReadingListPage readingListPage = new ReadingListPage(page);
        readingListPage.status(ReadingListPage.STATUS_QUEUE_FOR_SAVE);
        List<ReadingListPage> pagesListToBeAdded = new ArrayList<>();
        pagesListToBeAdded.add(readingListPage);
        ReadingList list = readingListDbHelper.createList("Test6", "TestDesc");
        readingListDbHelper.addPagesToList(list, pagesListToBeAdded, false);
        List<ReadingListPage> pagesList = readingListDbHelper.getAllPagesToBeSaved();
        readingListDbHelper.deleteList(list);
        List<ReadingListPage> pages = new ArrayList<>();
        pages.add(readingListPage);
        readingListDbHelper.markPagesForDeletion(list, pages);
        Assert.assertTrue(pagesList.size() != 0);
    }

    @Test
    public void testGetAllPagesToBeDeletedForRetrievingPages() {
        PageTitle page = new PageTitle("2", WikipediaApp.getInstance().getWikiSite());
        ReadingListPage readingListPage = new ReadingListPage(page);
        readingListPage.status(ReadingListPage.STATUS_QUEUE_FOR_DELETE);
        List<ReadingListPage> pagesListToBeAdded = new ArrayList<>();
        pagesListToBeAdded.add(readingListPage);
        ReadingList list = readingListDbHelper.createList("Test7", "TestDesc");
        readingListDbHelper.addPagesToList(list, pagesListToBeAdded, false);
        List<ReadingListPage> pagesList = readingListDbHelper.getAllPagesToBeDeleted();
        readingListDbHelper.deleteList(list);
        readingListDbHelper.markPagesForDeletion(list, pagesListToBeAdded);
        Assert.assertTrue(pagesList.size() != 0);
    }

    @Test
    public void testAddIfNotExistsForRetrievingPages() {
        ReadingList list = readingListDbHelper.createList("Test8", "TestDesc");
        PageTitle page = new PageTitle("1", WikipediaApp.getInstance().getWikiSite());
        PageTitle page2 = new PageTitle("2", WikipediaApp.getInstance().getWikiSite());
        PageTitle page3 = new PageTitle("3", WikipediaApp.getInstance().getWikiSite());
        readingListDbHelper.addPageToList(list, page, false);
        List<PageTitle> pages = new ArrayList<>();
        pages.add(page);
        pages.add(page2);
        pages.add(page3);
        int numAdded = readingListDbHelper.addPagesToListIfNotExist(list, pages);
        readingListDbHelper.deleteList(list);
        List<ReadingListPage> readingListPages = new ArrayList<>();
        for (PageTitle page1 : pages) {
            readingListPages.add(new ReadingListPage(page1));
        }
        readingListDbHelper.markPagesForDeletion(list, readingListPages);
        Assert.assertTrue(numAdded == 2);
    }

    @Test
    public void testFindPageForRetrievingPages() {
        ReadingList list = readingListDbHelper.createList("Test8", "TestDesc");
        PageTitle page = new PageTitle("1", WikipediaApp.getInstance().getWikiSite());
        PageTitle page2 = new PageTitle("2", WikipediaApp.getInstance().getWikiSite());
        PageTitle page3 = new PageTitle("3", WikipediaApp.getInstance().getWikiSite());
        readingListDbHelper.addPageToList(list, page, false);
        readingListDbHelper.addPageToList(list, page2, false);
        readingListDbHelper.addPageToList(list, page3, false);
        boolean exists = readingListDbHelper.pageExistsInList(list, page);
        readingListDbHelper.deleteList(list);
        List<ReadingListPage> pages = new ArrayList<>();
        pages.add(new ReadingListPage(page));
        pages.add(new ReadingListPage(page2));
        pages.add(new ReadingListPage(page3));
        readingListDbHelper.markPagesForDeletion(list, pages);
        Assert.assertTrue(exists);
    }

    @Test
    public void testFindPageForRetrievingPage() {
        ReadingList list = readingListDbHelper.createList("Test9", "TestDesc");
        ReadingList list2 = readingListDbHelper.createList("Test10", "TestDesc");
        PageTitle page = new PageTitle("1", WikipediaApp.getInstance().getWikiSite());
        PageTitle page2 = new PageTitle("2", WikipediaApp.getInstance().getWikiSite());
        readingListDbHelper.addPageToList(list, page, false);
        readingListDbHelper.addPageToList(list2, page2, false);
        ReadingListPage readingListPage = readingListDbHelper.findPageInAnyList(page);
        readingListDbHelper.deleteList(list);
        readingListDbHelper.deleteList(list2);
        List<ReadingListPage> pages = new ArrayList<>();
        pages.add(new ReadingListPage(page));
        readingListDbHelper.markPagesForDeletion(list, pages);
        pages.clear();
        pages.add(new ReadingListPage(page2));
        readingListDbHelper.markPagesForDeletion(list2, pages);
        Assert.assertTrue(readingListPage != null);
    }

    @Test
    public void testGetAllPageOccurrencesForRetrievingPages() {
        ReadingList list = readingListDbHelper.createList("Test11", "TestDesc");
        ReadingList list2 = readingListDbHelper.createList("Test12", "TestDesc");
        PageTitle page = new PageTitle("6", WikipediaApp.getInstance().getWikiSite());
        readingListDbHelper.addPageToList(list, page, false);
        readingListDbHelper.addPageToList(list2, page, false);
        int numOfPages = readingListDbHelper.getAllPageOccurrences(page).size();
        readingListDbHelper.deleteList(list);
        readingListDbHelper.deleteList(list2);
        List<ReadingListPage> pages = new ArrayList<>();
        pages.add(new ReadingListPage(page));
        readingListDbHelper.markPagesForDeletion(list, pages);
        readingListDbHelper.markPagesForDeletion(list2, pages);
        Assert.assertTrue(numOfPages != 0);
    }


    public boolean hasTitle(List<ReadingList> lists, String title) {
        List<String> titles = new ArrayList<>();
        for (ReadingList readingList : lists) {
            titles.add(readingList.title());
        }
        return titles.contains(title);
    }

    @After
    public void cleanUp() {
        readingListDbHelper.purgeDeletedPages();
    }
}
