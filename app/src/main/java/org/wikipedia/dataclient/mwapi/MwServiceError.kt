package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.util.ThrowableUtil.getBlockMessageHtml
import java.util.*

@JsonClass(generateAdapter = true)
class MwServiceError(val code: String = "", var html: String = "", val data: Data? = null) : ServiceError {
    val isBadToken: Boolean
        get() = "badtoken" == code
    val isBadLoginState: Boolean
        get() = "assertuserfailed" == code

    init {
        // Special case: if it's a Blocked error, parse the blockinfo structure ourselves.
        if (("blocked" == title || "autoblocked" == title) && data?.blockinfo != null) {
            html = getBlockMessageHtml(data.blockinfo)
        }
    }

    fun hasMessageName(messageName: String): Boolean {
        return data?.messages?.any { it.name == messageName } ?: false
    }

    fun getMessageHtml(messageName: String): String? {
        return data?.messages?.filter { it.name == messageName }?.map { it.html }?.firstOrNull()
    }

    override val title: String get() = code

    override val details: String get() = html

    @JsonClass(generateAdapter = true)
    class Data(val messages: List<Message> = emptyList(), val blockinfo: BlockInfo? = null)

    @JsonClass(generateAdapter = true)
    class Message(val name: String = "", val html: String = "")

    @JsonClass(generateAdapter = true)
    open class BlockInfo(
        @Json(name = "blockid") val blockId: Int = 0,
        @Json(name = "blockedbyid") val blockedById: Int = 0,
        @Json(name = "blockreason") val blockReason: String = "",
        @Json(name = "blockedby") val blockedBy: String = "",
        @Json(name = "blockedtimestamp") val blockedTimeStamp: Date = Date(0),
        @Json(name = "blockexpiry") val blockExpiry: Date = Date(0)
    ) {
        val isBlocked: Boolean
            get() = blockExpiry.after(Date())
    }
}
