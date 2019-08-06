package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;
import org.wikipedia.page.Namespace;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.json.GsonMarshaller.marshal;
import static org.wikipedia.json.GsonUnmarshaller.unmarshal;

@RunWith(ParameterizedRobolectricTestRunner.class) public class NamespaceTypeAdapterTest {
    @Parameters(name = "{0}") public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {{DeferredParam.NULL}, {DeferredParam.SPECIAL},
                {DeferredParam.MAIN}, {DeferredParam.TALK}});
    }

    @Nullable private final Namespace namespace;

    public NamespaceTypeAdapterTest(@NonNull DeferredParam param) {
        this.namespace = param.val();
    }

    @Test public void testWriteRead() {
        Namespace result = unmarshal(Namespace.class, marshal(namespace));
        assertThat(result, is(namespace));
    }

    @Test public void testReadOldData() {
        // Prior to 3210ce44, we marshaled Namespace as the name string of the enum, instead of
        // the code number, and when we switched to using the code number, we didn't introduce
        // backwards-compatible checks for the old-style strings that may still be present in
        // some local serialized data.
        // TODO: remove after April 2017?
        String marshaledStr = namespace == null ? "null" : "\"" + namespace.name() + "\"";
        Namespace ns = unmarshal(Namespace.class, marshaledStr);
        assertThat(ns, is(namespace));
    }

    // SparseArray is a Roboelectric mocked class which is unavailable at static time; defer
    // evaluation until TestRunner is executed
    private enum DeferredParam {
        NULL() {
            @Nullable @Override
            Namespace val() {
                return null;
            }
        },
        SPECIAL() {
            @NonNull @Override Namespace val() {
                return Namespace.SPECIAL;
            }
        },
        MAIN() {
            @NonNull @Override Namespace val() {
                return Namespace.MAIN;
            }
        },
        TALK() {
            @NonNull @Override Namespace val() {
                return Namespace.TALK;
            }
        };

        @Nullable abstract Namespace val();
    }
}
