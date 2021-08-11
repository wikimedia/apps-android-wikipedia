package org.wikipedia.json;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.Service;
import org.wikipedia.json.annotations.Required;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(RobolectricTestRunner.class)
public class RequiredFieldsCheckOnReadJsonAdapterFactoryTest {
    @Test
    public void testRequireNonNull() throws IOException {
        final JsonAdapter<RequiredModel> adapter = MoshiUtil.getDefaultMoshi().adapter(RequiredModel.class);
        final RequiredModel expected = new RequiredModel();
        expected.field = 1;
        final RequiredModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.field, is(expected.field));
    }

    @Test
    public void testRequireNull() throws IOException {
        final JsonAdapter<RequiredModel> adapter = MoshiUtil.getDefaultMoshi().adapter(RequiredModel.class);
        final RequiredModel model = new RequiredModel();
        final RequiredModel result = adapter.fromJson(adapter.toJson(model));
        assertThat(result, nullValue());
    }

    @Test
    public void testRequireMissing() throws IOException {
        final JsonAdapter<RequiredModel> adapter = MoshiUtil.getDefaultMoshi().adapter(RequiredModel.class);
        final RequiredModel result = adapter.fromJson("{}");
        assertThat(result, nullValue());
    }

    @Test
    public void testOptionalNonNull() throws IOException {
        final JsonAdapter<OptionalModel> adapter = MoshiUtil.getDefaultMoshi().adapter(OptionalModel.class);
        final OptionalModel expected = new OptionalModel();
        expected.field = 1;
        final OptionalModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.field, is(expected.field));
    }

    @Test
    public void testOptionalNull() throws IOException {
        final JsonAdapter<OptionalModel> adapter = MoshiUtil.getDefaultMoshi().adapter(OptionalModel.class);
        final OptionalModel expected = new OptionalModel();
        final OptionalModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.field, is(expected.field));
    }

    @Test
    public void testOptionalMissing() throws IOException {
        final JsonAdapter<OptionalModel> adapter = MoshiUtil.getDefaultMoshi().adapter(OptionalModel.class);
        final OptionalModel expected = new OptionalModel();
        final OptionalModel result = adapter.fromJson("{}");
        assert result != null;
        assertThat(result.field, is(expected.field));
    }

    @Test
    public void testRequiredTypeAdapterNonNull() throws IOException {
        final JsonAdapter<RequiredTypeAdapterModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(RequiredTypeAdapterModel.class);
        final RequiredTypeAdapterModel expected = new RequiredTypeAdapterModel();
        expected.uri = Uri.parse(Service.WIKIPEDIA_URL);
        final RequiredTypeAdapterModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.uri, is(expected.uri));
    }

    @Test
    public void testRequiredTypeAdapterNull() throws IOException {
        final JsonAdapter<RequiredTypeAdapterModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(RequiredTypeAdapterModel.class);
        final RequiredTypeAdapterModel expected = new RequiredTypeAdapterModel();
        final RequiredTypeAdapterModel result = adapter.fromJson(adapter.toJson(expected));
        assertThat(result, nullValue());
    }

    @Test
    public void testRequiredTypeAdapterMissing() throws IOException {
        final JsonAdapter<RequiredTypeAdapterModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(RequiredTypeAdapterModel.class);
        final RequiredTypeAdapterModel result = adapter.fromJson("{}");
        assertThat(result, nullValue());
    }

    @Test
    public void testOptionalTypeAdapterNonNull() throws IOException {
        final JsonAdapter<OptionalTypeAdapterModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(OptionalTypeAdapterModel.class);
        OptionalTypeAdapterModel expected = new OptionalTypeAdapterModel();
        expected.uri = Uri.parse(Service.WIKIPEDIA_URL);
        OptionalTypeAdapterModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.uri, is(expected.uri));
    }

    @Test
    public void testOptionalTypeAdapterNull() throws IOException {
        final JsonAdapter<OptionalTypeAdapterModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(OptionalTypeAdapterModel.class);
        final OptionalTypeAdapterModel expected = new OptionalTypeAdapterModel();
        final OptionalTypeAdapterModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.uri, is(expected.uri));
    }

    @Test
    public void testOptionalTypeAdapterMissing() throws IOException {
        final JsonAdapter<OptionalTypeAdapterModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(OptionalTypeAdapterModel.class);
        final OptionalTypeAdapterModel expected = new OptionalTypeAdapterModel();
        final OptionalTypeAdapterModel result = adapter.fromJson("{}");
        assert result != null;
        assertThat(result.uri, is(expected.uri));
    }

    @Test
    public void testRequiredSerializedNameNonNull() throws IOException {
        final JsonAdapter<SerializedNameModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(SerializedNameModel.class);
        final SerializedNameModel expected = new SerializedNameModel();
        expected.bar = "hello world";
        final SerializedNameModel result = adapter.fromJson(adapter.toJson(expected));
        assert result != null;
        assertThat(result.bar, is(expected.bar));
    }

    @Test
    public void testRequiredSerializedNameNull() throws IOException {
        final JsonAdapter<SerializedNameModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(SerializedNameModel.class);
        final SerializedNameModel expected = new SerializedNameModel();
        final SerializedNameModel result = adapter.fromJson(adapter.toJson(expected));
        assertThat(result, nullValue());
    }

    @Test
    public void testRequiredSerializedNameMissing() throws IOException {
        final JsonAdapter<SerializedNameModel> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(SerializedNameModel.class);
        final SerializedNameModel result = adapter.fromJson("{}");
        assertThat(result, nullValue());
    }

    @Test
    public void testComposedValid() throws IOException {
        final JsonAdapter<ComposedModel> adapter = MoshiUtil.getDefaultMoshi().adapter(ComposedModel.class);
        final RequiredModel required = new RequiredModel();
        required.field = 1;
        final OptionalModel optional = new OptionalModel();
        final ComposedModel expected = new ComposedModel();
        expected.required = required;
        expected.optional = optional;

        final ComposedModel result = adapter.fromJson(adapter.toJson(expected));
        assertThat(result.optional.field, is(expected.optional.field));
        assertThat(result.required.field, is(expected.required.field));
    }

    @Test
    public void testComposedInvalid() throws IOException {
        final JsonAdapter<ComposedModel> adapter = MoshiUtil.getDefaultMoshi().adapter(ComposedModel.class);
        final RequiredModel required = new RequiredModel();
        final OptionalModel optional = new OptionalModel();
        final ComposedModel aggregated = new ComposedModel();
        aggregated.required = required;
        aggregated.optional = optional;

        final ComposedModel result = adapter.fromJson(adapter.toJson(aggregated));
        assertThat(result, nullValue());
    }

    private static class RequiredModel {
        @SuppressWarnings("NullableProblems") @Required @NonNull private Integer field;
    }

    private static class OptionalModel {
        @Nullable private Integer field;
    }

    private static class ComposedModel {
        @SuppressWarnings("NullableProblems") @Required @NonNull private RequiredModel required;
        @Nullable private OptionalModel optional;
    }

    private static class RequiredTypeAdapterModel {
        @SuppressWarnings("NullableProblems") @Required @NonNull private Uri uri;
    }

    private static class OptionalTypeAdapterModel {
        @Nullable private Uri uri;
    }

    private static class SerializedNameModel {
        @SuppressWarnings("NullableProblems") @Json(name = "foo") @Required @NonNull private String bar;
    }
}
