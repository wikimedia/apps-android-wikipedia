package org.wikipedia.readinglist.page.database;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.test.TestRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// TODO: expand!
@RunWith(TestRunner.class) public class ReadingListPageDaoTest {

    @Test @SuppressWarnings("checkstyle:magicnumber") public void getSelectRowsWithKeysString() throws Throwable {
        assertThat(ReadingListPageDao.Sql.getSelectRowsWithKeysString(0),
                is(":keyCol == ?".replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName())));

        assertThat(ReadingListPageDao.Sql.getSelectRowsWithKeysString(1),
                is(":keyCol == ?".replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName())));

        assertThat(ReadingListPageDao.Sql.getSelectRowsWithKeysString(2),
                is(":keyCol IN (?,?)".replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName())));

        assertThat(ReadingListPageDao.Sql.getSelectRowsWithKeysString(3),
                is(":keyCol IN (?,?,?)".replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName())));
    }

    @Test(expected = IllegalArgumentException.class) public void testGetSelectRowsWithKeysStringIllegalArg() {
        ReadingListPageDao.Sql.getSelectRowsWithKeysString(-1);
    }
}
