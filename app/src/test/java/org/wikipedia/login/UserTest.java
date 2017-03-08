package org.wikipedia.login;

import android.support.v4.util.ArraySet;

import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.TestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(TestRunner.class)
public class UserTest {
    private static final Set<String> GROUPS
            = Collections.unmodifiableSet(new ArraySet<>(Arrays.asList("*", "user", "autoconfirmed")));
    private TypeToken<Map<String, Integer>> type = new TypeToken<Map<String, Integer>>(){};

    @Before @SuppressWarnings("checkstyle:magicnumber") public void setUp() {
        User.disableStorage();

        User user = new User("name", "pwd", new HashMap<String, Integer>(), GROUPS);
        User.setUser(user);

        user.putIdForLanguage("en", 333);
        user.putIdForLanguage("es", 444);
        user.putIdForLanguage("de", 555);
    }

    @Test public void testUserLogin() {
        User user = User.getUser();
        //noinspection ConstantConditions
        assertThat(user.getUsername(), is("name"));
        assertThat(user.getPassword(), is("pwd"));
        assertThat(user.getIdMap(), is(GsonUnmarshaller.unmarshal(type, "{\"en\":333,\"es\":444,\"de\":555}")));
        assertThat(user.getGroupMemberships(), is(GROUPS));
    }

    @Test @SuppressWarnings("checkstyle:magicnumber") public void testGetIdsByLanguage() {
        User user = User.getUser();
        //noinspection ConstantConditions
        assertThat(user.getIdForLanguage("en"), is(333));
        assertThat(user.getIdForLanguage("es"), is(444));
        assertThat(user.getIdForLanguage("de"), is(555));
    }

    @Test @SuppressWarnings("checkstyle:magicnumber") public void testMarshalAndUnmarshalIds() {
        User user = User.getUser();
        //noinspection ConstantConditions
        String json = GsonMarshaller.marshal(user.getIdMap());

        Map<String, Integer> ids = GsonUnmarshaller.unmarshal(type, json);
        assertThat(ids.size(), is(3));
        assertThat(ids.get("en"), is(333));
        assertThat(ids.get("es"), is(444));
        assertThat(ids.get("de"), is(555));
    }

    @Test public void testClear() {
        User.clearUser();
        assertThat(User.getUser(), is(nullValue()));
    }
}
