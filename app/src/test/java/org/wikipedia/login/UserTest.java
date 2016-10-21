package org.wikipedia.login;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(TestRunner.class)
public class UserTest {
    private static final int USER_ID = 333;
    private static final Set<String> GROUPS
            = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("*", "user", "autoconfirmed")));

    @Before
    public void setUp() {
        User.disableStorage();

        User user = new User("name", "pwd", USER_ID, GROUPS);
        User.setUser(user);
    }

    @Test
    public void testUserLogin() {
        User user = User.getUser();
        //noinspection ConstantConditions
        assertThat(user.getUsername(), is("name"));
        assertThat(user.getPassword(), is("pwd"));
        assertThat(user.getUserID(), is(USER_ID));
        assertThat(user.getGroupMemberships(), is(GROUPS));
    }

    @Test
    public void testClear() {
        User.clearUser();
        assertThat(User.getUser(), is(nullValue()));
    }
}
