package org.wikipedia.search;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResult;
import org.wikipedia.json.GsonUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SearchResultsRedirectProcessingTest {

    private MwQueryResult result;

    @Before public void setUp() {
        result = GsonUtil.getDefaultGson().fromJson(queryJson, MwQueryResult.class);
    }

    @Test public void testRedirectHandling() {
        assertThat(result.getPages().size(), is(2));
        assertThat(result.getPages().get(0).getTitle(), is("Narthecium#Foo"));
        assertThat(result.getPages().get(0).getRedirectFrom(), is("Abama"));
        assertThat(result.getPages().get(1).getTitle(), is("Amitriptyline"));
        assertThat(result.getPages().get(1).getRedirectFrom(), is("Abamax"));
    }

    @Test public void testConvertTitleHandling() {
        assertThat(result.getPages().size(), is(2));
        assertThat(result.getPages().get(0).getTitle(), is("Narthecium#Foo"));
        assertThat(result.getPages().get(0).getConvertedFrom(), is("NotNarthecium"));
    }

    private String queryJson = "{\n"
            + "    \"converted\": [\n"
            + "      {\n"
            + "        \"from\": \"NotNarthecium\",\n"
            + "        \"to\": \"Narthecium\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"redirects\": [\n"
            + "      {\n"
            + "        \"index\": 1,\n"
            + "        \"from\": \"Abama\",\n"
            + "        \"to\": \"Narthecium\",\n"
            + "        \"tofragment\": \"Foo\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"index\": 2,\n"
            + "        \"from\": \"Abamax\",\n"
            + "        \"to\": \"Amitriptyline\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"pages\":[\n"
            + "      {\n"
            + "        \"pageid\": 2060913,\n"
            + "        \"ns\": 0,\n"
            + "        \"title\": \"Narthecium\",\n"
            + "        \"index\": 1,\n"
            + "        \"terms\": {\n"
            + "          \"description\": [\n"
            + "            \"genus of plants\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"thumbnail\": {\n"
            + "          \"source\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Narthecium_ossifragum_01.jpg/240px-Narthecium_ossifragum_01.jpg\",\n"
            + "          \"width\": 240,\n"
            + "          \"height\": 320\n"
            + "        }\n"
            + "      },\n"
            + "      {\n"
            + "        \"pageid\": 583678,\n"
            + "        \"ns\": 0,\n"
            + "        \"title\": \"Amitriptyline\",\n"
            + "        \"index\": 2,\n"
            + "        \"terms\": {\n"
            + "          \"description\": [\n"
            + "            \"chemical compound\",\n"
            + "            \"chemical compound\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"thumbnail\": {\n"
            + "          \"source\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Amitriptyline2DACS.svg/318px-Amitriptyline2DACS.svg.png\",\n"
            + "          \"width\": 318,\n"
            + "          \"height\": 320\n"
            + "        }\n"
            + "      }\n"
            + "    ]\n"
            + "  }";

}
