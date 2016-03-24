package org.wikipedia.readinglist.api.legacy;

import org.wikipedia.dataclient.RetrofitClientBaseTest;
import org.wikipedia.test.TestFileUtil;
import org.wikipedia.test.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(TestRunner.class)
public class GetLegacyReadingListPageTitlesTest extends RetrofitClientBaseTest {
    private static final int WATCHLIST_ID = 0;

    @Test
    public void testGetCollectionPagesNotLoggedIn() throws Exception {
        getCollectionPages(TestFileUtil.readRawFile("gather_not_logged_in.json"), null);
    }

    @Test
    public void testCollectionPages() throws Exception {
        MockLegacyReadingListPageTitlesResponse expected = new MockLegacyReadingListPageTitlesResponse();
        List<LegacyReadingListPageTitle> list = new ArrayList<>();
        list.add(new MockLegacyReadingListPageTitle(0, "Test"));
        list.add(new MockLegacyReadingListPageTitle(1, "Talk:Test"));
        expected.setQuery(new MockPageTitlesList(list));

        getCollectionPages(TestFileUtil.readRawFile("gather_get_collection_pages.json"),
                expected);
    }

    private void getCollectionPages(String responseBody,
                                    @Nullable LegacyReadingListPageTitlesResponse expected)
            throws Exception {
        runTest(responseBody, new GetCollectionPagesSubject(expected));
    }

    private class GetCollectionPagesSubject extends BaseTestSubject {
        @Nullable
        private final LegacyReadingListPageTitlesResponse expected;

        GetCollectionPagesSubject(@Nullable LegacyReadingListPageTitlesResponse expected) {
            super();
            this.expected = expected;
        }

        @Override
        public void execute() {
            LegacyReadingListPageTitlesResponse actual = getClient().getMemberPages(WATCHLIST_ID);
            if (expected != null) {
                List<LegacyReadingListPageTitle> act = actual.query().getMemberPages();
                List<LegacyReadingListPageTitle> exp = expected.query().getMemberPages();
                assertThat(act.size(), is(exp.size()));
                for (int i = 0; i < act.size(); i++) {
                    assertPageIsEqual(act.get(i), exp.get(i), i);
                }
            } else {
                assertThat(actual.getError().getTitle(), is("lstnotloggedin"));
            }
        }

        private void assertPageIsEqual(LegacyReadingListPageTitle act, LegacyReadingListPageTitle exp,
                                       int index) {
            assertThat("namespaceId mismatch in index " + index,
                    act.getNamespaceId(), is(exp.getNamespaceId()));
            assertThat("title mismatch in index " + index,
                    act.getPrefixedTitle(), is(exp.getPrefixedTitle()));
        }
    }

    /** This class was added to allow accessing #setQuery from this test. */
    private static final class MockLegacyReadingListPageTitlesResponse
            extends LegacyReadingListPageTitlesResponse {
        @Override
        protected void setQuery(@Nullable LegacyReadingListPageTitlesResponse.ListPages query) {
            super.setQuery(query);
        }
    }

    private static final class MockPageTitlesList
            extends LegacyReadingListPageTitlesResponse.ListPages {

        private final List<LegacyReadingListPageTitle> mockList;

        private MockPageTitlesList(List<LegacyReadingListPageTitle> list) {
            this.mockList = list;
        }

        @Override
        public List<LegacyReadingListPageTitle> getMemberPages() {
            return mockList;
        }
    }

    private static final class MockLegacyReadingListPageTitle extends LegacyReadingListPageTitle {
        private final int mockNamespaceId;
        private final String mockTitle;

        MockLegacyReadingListPageTitle(int nameSpaceId, String prefixedTitle) {
            mockNamespaceId = nameSpaceId;
            mockTitle = prefixedTitle;
        }

        @Override
        public int getNamespaceId() {
            return mockNamespaceId;
        }

        @Override
        public String getPrefixedTitle() {
            return mockTitle;
        }
    }
}
