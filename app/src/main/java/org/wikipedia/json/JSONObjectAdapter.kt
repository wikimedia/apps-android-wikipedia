package org.wikipedia.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer
import org.json.JSONException
import org.json.JSONObject

class JSONObjectAdapter : JsonAdapter<JSONObject>() {
    override fun fromJson(reader: JsonReader): JSONObject? {
        return (reader.readJsonValue() as? Map<String, Any>)?.let {
            try {
                JSONObject(it)
            } catch (e: JSONException) {
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: JSONObject?) {
        writer.value(Buffer().writeUtf8(value.toString()))
    }
}
