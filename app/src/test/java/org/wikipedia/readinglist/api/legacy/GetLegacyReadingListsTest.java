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
public class GetLegacyReadingListsTest extends RetrofitClientBaseTest {

    private static final int LIST_ID = 11;
    private static final int COUNT = 4;

    @Test
    public void testGetCollectionsNotLoggedIn() throws Exception {
        getCollections(TestFileUtil.readRawFile("gather_not_logged_in.json"), null);
    }

    @Test
    public void testGetCollections() throws Exception {
        MockLegacyReadingListsResponse expected = new MockLegacyReadingListsResponse();
        List<LegacyReadingList> lists = new ArrayList<>();

        LegacyReadingList collection1 = new MockLegacyReadingList(0, "Watchlist",
                "someUser", "private", "", "2016-02-19T00:05:07Z", 1);
        lists.add(collection1);

        MockLegacyReadingList collection2 = new MockLegacyReadingList(LIST_ID, "firstlist",
                "someUser", "public", "a simple test list", "2016-01-06T20:19:58Z", COUNT);
        collection2.setImageUrl("//upload.wikimedia.org/wikipedia/commons/7/79/sample_picture.jpg");
        lists.add(collection2);
        expected.setQuery(new MockResponseList(lists));

        getCollections(TestFileUtil.readRawFile("gather_get_collections.json"), expected);
    }

    private void getCollections(String responseBody, @Nullable LegacyReadingListsResponse expected)
            throws Exception {
        runTest(responseBody, new GetCollectionsSubject(expected));
    }

    private class GetCollectionsSubject extends BaseTestSubject {
        @Nullable
        private final LegacyReadingListsResponse expected;

        GetCollectionsSubject(@Nullable LegacyReadingListsResponse expected) {
            super();
            this.expected = expected;
        }

        @Override
        public void execute() {
            LegacyReadingListsResponse actual = getClient().getReadingLists();
            if (expected != null) {
                List<LegacyReadingList> act = actual.query().getLists();
                List<LegacyReadingList> exp = expected.query().getLists();
                assertThat(act.size(), is(exp.size()));
                for (int i = 0; i < act.size(); i++) {
                    assertThatCollectionsAreEqual(act.get(i), exp.get(i), i);
                }
            } else {
                assertThat(actual.getError().getTitle(), is("lstnotloggedin"));
            }
        }

        private void assertThatCollectionsAreEqual(LegacyReadingList act, LegacyReadingList exp,
                                                   int index) {
            assertThat("index " + index, act.getId(), is(exp.getId()));
            assertThat("index " + index, act.getLabel(), is(exp.getLabel()));
            assertThat("index " + index, act.getOwner(), is(exp.getOwner()));
            assertThat("index " + index, act.getPerm(), is(exp.getPerm()));
            assertThat("index " + index, act.getDescription(), is(exp.getDescription()));
            assertThat("index " + index, act.getLastUpdated(), is(exp.getLastUpdated()));
            assertThat("index " + index, act.getCount(), is(exp.getCount()));
            assertThat("index " + index, act.getImageUrl(), is(exp.getImageUrl()));
        }
    }

    /** This class was added to allow accessing #setQuery from this test. */
    private static final class MockLegacyReadingListsResponse extends LegacyReadingListsResponse {
        @Override
        protected void setQuery(@Nullable LegacyLists query) {
            super.setQuery(query);
        }
    }

    private static final class MockResponseList extends LegacyReadingListsResponse.LegacyLists {
        private final List<LegacyReadingList> mockList;

        MockResponseList(List<LegacyReadingList> list) {
            this.mockList = list;
        }

        @Override
        public List<LegacyReadingList> getLists() {
            return mockList;
        }
    }

    private static final class MockLegacyReadingList extends LegacyReadingList {
        MockLegacyReadingList(int id, String label, String owner, String perm, String description,
                              String updated, int count) {
            this.id = id;
            this.label = label;
            this.owner = owner;
            this.perm = perm;
            this.description = description;
            this.updated = updated;
            this.count = count;
        }

        void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
