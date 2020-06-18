package org.wikipedia.pageimages;

import androidx.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockRetrofitTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;

public class PageImagesClientTest extends MockRetrofitTest {
    private static final WikiSite WIKISITE_TEST = WikiSite.forLanguageCode("test");
    private static final PageTitle PAGE_TITLE_BIDEN = new PageTitle("Joe Biden", WIKISITE_TEST);
    private static final PageTitle PAGE_TITLE_OBAMA = new PageTitle("Barack Obama", WIKISITE_TEST);

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("reading_list_page_info.json");
        List<PageTitle> titles = new ArrayList<>();
        titles.add(PAGE_TITLE_OBAMA);
        titles.add(PAGE_TITLE_BIDEN);

        getObservable(titles).test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> {
                    PageImage biden = result.get(PAGE_TITLE_BIDEN);
                    PageImage obama = result.get(PAGE_TITLE_OBAMA);
                    return biden.getTitle().getPrefixedText().equals("Joe_Biden")
                            && biden.getImageName().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Official_portrait_of_Vice_President_Joe_Biden.jpg/255px-Official_portrait_of_Vice_President_Joe_Biden.jpg")
                            && obama.getTitle().getPrefixedText().equals("Barack_Obama")
                            && obama.getImageName().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg");
                });
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable(Collections.emptyList()).test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        getObservable(Collections.emptyList()).test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable(Collections.emptyList()).test().await()
                .assertError(MalformedJsonException.class);
    }

    private Observable<Map<PageTitle, PageImage>> getObservable(@NonNull List<PageTitle> titles) {
        return getApiService().getPageImages("foo")
                .map(response -> PageImage.imageMapFromPages(WIKISITE_TEST, titles, response.query().pages()));
    }
}
