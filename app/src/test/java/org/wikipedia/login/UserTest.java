package org.wikipedia.login;

import org.wikipedia.test.TestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(TestRunner.class)
public class UserTest {
    private static final int USER_ID = 333;

    @Before
    public void setUp() {
        User.disableStorage();

        User user = new User("name", "pwd", USER_ID);
        User.setUser(user);
    }

    @Test
    public void testUserLogin() {
        User user2 = User.getUser();
        //noinspection ConstantConditions
        assertThat(user2.getUsername(), is("name"));
        assertThat(user2.getPassword(), is("pwd"));
        assertThat(user2.getUserID(), is(USER_ID));
    }

    @Test
    public void testClear() {
        User.clearUser();
        assertThat(User.getUser(), is(nullValue()));
    }
}
