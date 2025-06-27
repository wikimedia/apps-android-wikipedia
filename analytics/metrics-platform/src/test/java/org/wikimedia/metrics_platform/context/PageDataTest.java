package org.wikimedia.metrics_platform.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.json.GsonHelper;

import com.google.gson.Gson;

class PageDataTest {

    @Test void testPageData() {
        PageData pageData = PageData.builder()
                .id(1)
                .title("Test")
                .namespaceId(0)
                .namespaceName("")
                .revisionId(1L)
                .wikidataItemQid("Q1")
                .contentLanguage("zh")
                .build();

        assertThat(pageData.getId()).isEqualTo(1);
        assertThat(pageData.getNamespaceId()).isEqualTo(0);
        assertThat(pageData.getNamespaceName()).isEmpty();
        assertThat(pageData.getTitle()).isEqualTo("Test");
        assertThat(pageData.getRevisionId()).isEqualTo(1);
        assertThat(pageData.getWikidataItemQid()).isEqualTo("Q1");
        assertThat(pageData.getContentLanguage()).isEqualTo("zh");

        Gson gson = GsonHelper.getGson();
        String json = gson.toJson(pageData);
        assertThat(json).isEqualTo("{\"id\":1," +
                "\"title\":\"Test\"," +
                "\"namespace_id\":0," +
                "\"namespace_name\":\"\"," +
                "\"revision_id\":1," +
                "\"wikidata_qid\":\"Q1\"," +
                "\"content_language\":\"zh\"" +
                "}");
    }

}
