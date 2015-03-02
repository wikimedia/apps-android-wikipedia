package org.wikipedia.page.bottomcontent;

import org.wikipedia.PageTitle;

public interface BottomContentInterface {

    void hide();
    void beginLayout();
    PageTitle getTitle();
    void setTitle(PageTitle newTitle);

}
