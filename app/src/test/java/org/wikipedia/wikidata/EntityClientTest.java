package org.wikipedia.wikidata;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockWebServerTest;
import org.wikipedia.test.TestFileUtil;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EntityClientTest extends MockWebServerTest {
    @NonNull private EntityClient client = EntityClient.instance();

    @Test public void testRequestLabelSuccess() throws Throwable {
        enqueueFromFile("wikidata_entity_label.json");
        EntityClient.LabelCallback cb = mock(EntityClient.LabelCallback.class);
        request(cb, "Q123", "en");
        server().takeRequest();
        verify(cb).success(any(String.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Throwable.class));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        EntityClient.LabelCallback cb = mock(EntityClient.LabelCallback.class);
        request(cb, "Q123", "en");
        server().takeRequest();
        verify(cb, never()).success(any(String.class));
        verify(cb).failure(any(Throwable.class));
    }

    @Test public void testRequestLabelInvalidLang() throws Throwable {
        enqueueFromFile("wikidata_entity_label.json");
        EntityClient.LabelCallback cb = mock(EntityClient.LabelCallback.class);
        request(cb, "Q123", "ru");
        server().takeRequest();
        verify(cb, never()).success(any(String.class));
        verify(cb).failure(any(Throwable.class));
    }

    @Test public void testRequestLabelInvalidEntity() throws Throwable {
        enqueueFromFile("wikidata_entity_label_invalid_entity.json");
        EntityClient.LabelCallback cb = mock(EntityClient.LabelCallback.class);
        request(cb, "Q123", "en");
        server().takeRequest();
        verify(cb, never()).success(any(String.class));
        verify(cb).failure(any(Throwable.class));
    }

    @Test public void testRequestMalformed() throws Throwable {
        server().enqueue("(╯°□°）╯︵ ┻━┻");
        EntityClient.LabelCallback cb = mock(EntityClient.LabelCallback.class);
        request(cb, "Q123", "en");
        server().takeRequest();
        verify(cb, never()).success(any(String.class));
        verify(cb).failure(any(Throwable.class));
    }

    @Test public void testLabel() throws Throwable {
        String json = TestFileUtil.readRawFile("wikidata_entity_label.json");
        Entities response
                = GsonUnmarshaller.unmarshal(Entities.class, json);
        Entities.Entity entity = response.entities().get("Q123");
        assertThat(entity.id(), is("Q123"));
        assertThat(entity.labels().get("en").value(), is("September"));
    }

    private void request(@NonNull final EntityClient.LabelCallback cb,
                         @NonNull final String qNumber, @NonNull final String langCode) {
        Call<Entities> call = client.requestLabels(service(EntityClient.Service.class), qNumber, langCode);
        call.enqueue(new EntityClient.LabelCallbackAdapter(cb, qNumber, langCode));
    }
}
