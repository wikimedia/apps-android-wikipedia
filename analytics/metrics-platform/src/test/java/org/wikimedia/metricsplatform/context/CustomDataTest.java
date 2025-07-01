package org.wikimedia.metricsplatform.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.metricsplatform.context.DataFixtures.getTestCustomData;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.json.GsonHelper;

import com.google.gson.Gson;

class CustomDataTest {

//    @Test void testFormatCustomDataByCustomSerialization() {
//        Map<String, CustomData> customData = getTestCustomDataFormatted();
//
//        Gson gson = GsonHelper.getGson();
//        String jsonCustomData = gson.toJson(customData);
//
//        assertThat(jsonCustomData)
//            .isEqualTo("{" +
//                    "\"is_full_width\":" +
//                    "{" +
//                    "\"data_type\":\"boolean\"," +
//                    "\"value\":\"true\"" +
//                    "}," +
//                    "\"font_size\":" +
//                    "{" +
//                    "\"data_type\":\"string\"," +
//                    "\"value\":\"small\"" +
//                    "}," +
//                    "\"screen_size\":" +
//                    "{" +
//                    "\"data_type\":\"number\"," +
//                    "\"value\":\"1080\"" +
//                    "}" +
//                    "}");
//    }

    @Test void testCustomDataSerialization() {
        Map<String, Object> customData = getTestCustomData();

        Gson gson = GsonHelper.getGson();
        String jsonCustomData = gson.toJson(customData);

        assertThat(jsonCustomData)
                .isEqualTo("{" +
                        "\"is_full_width\":true," +
                        "\"font_size\":\"small\"," +
                        "\"screen_size\":1080" +
                        "}");
    }
}
