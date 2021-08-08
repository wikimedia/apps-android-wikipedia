package org.wikipedia.json;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;
import org.wikipedia.dataclient.Service;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(ParameterizedRobolectricTestRunner.class) public class UriJsonAdapterTest {
    @Parameters(name = "{0}") public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {{DeferredParam.NULL}, {DeferredParam.STRING},
                {DeferredParam.OPAQUE}, {DeferredParam.HIERARCHICAL}});
    }

    @Nullable private final Uri uri;

    public UriJsonAdapterTest(@NonNull DeferredParam param) {
        this.uri = param.val();
    }

    @Test public void testWriteRead() throws IOException {
        final JsonAdapter<Uri> adapter = MoshiUtil.getDefaultMoshi().adapter(Uri.class);
        Uri result = adapter.fromJson(adapter.toJson(uri));
        assertThat(result, is(uri));
    }

    // Namespace uses a roboelectric mocked class internally, SparseArray, which is unavailable at
    // static time; defer evaluation until TestRunner is executed
    private enum DeferredParam {
        NULL() {
            @Nullable @Override Uri val() {
                return null;
            }
        },
        STRING() {
            @Nullable @Override Uri val() {
                return Uri.parse(Service.WIKIPEDIA_URL);
            }
        },
        OPAQUE() {
            @Nullable @Override Uri val() {
                return Uri.fromParts("http", "mediawiki.org", null);
            }
        },
        HIERARCHICAL() {
            @Nullable @Override Uri val() {
                return Uri.EMPTY;
            }
        };

        @Nullable abstract Uri val();
    }
}
