package org.wikipedia.readinglist;

import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

public class ReadingList {
    public static final ReadingListData.ReadingListDao DAO = new ReadingListFakeData();

    private String title;
    private String description;
    private boolean saveOffline = true;
    private List<PageTitle> pages;

    public ReadingList() {
        this("", "", new ArrayList<PageTitle>());
    }

    public ReadingList(String title, String description, List<PageTitle> pages) {
        this.title = title;
        this.description = description;
        this.pages = pages;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PageTitle> getPages() {
        return pages;
    }

    public boolean getSaveOffline() {
        return saveOffline;
    }

    public void setSaveOffline(boolean saveOffline) {
        this.saveOffline = saveOffline;
    }
}
