package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.util.DateUtil.iso8601DateParse
import org.wikipedia.util.ThrowableUtil.getBlockMessageHtml
import java.util.*

class MwServiceError : ServiceError, PostProcessable {

    private var code: String? = null
    private val text: String? = null
    private var html: String? = null
    private val data: Data? = null

    constructor()
    constructor(code: String?, html: String?) {
        this.code = code
        this.html = html
    }

    override val title: String get() = code.orEmpty()

    override val details: String get() = html.orEmpty()

    fun badToken(): Boolean {
        return "badtoken" == code
    }

    fun badLoginState(): Boolean {
        return "assertuserfailed" == code
    }

    fun hasMessageName(messageName: String): Boolean {
        data?.messages?.let {
            it.forEach { msg ->
                if (messageName == msg.name) {
                    return true
                }
            }
        }
        return false
    }

    fun getMessageHtml(messageName: String): String? {
        data?.messages?.let {
            it.forEach { msg ->
                if (messageName == msg.name) {
                    return msg.html
                }
            }
        }
        return null
    }

    override fun postProcess() {
        // Special case: if it's a Blocked error, parse the blockinfo structure ourselves.
        if (("blocked" == code || "autoblocked" == code) && data != null && data.blockinfo != null) {
            html = getBlockMessageHtml(data.blockinfo)
        }
    }

    private class Data {
        val messages: List<Message>? = null
        val blockinfo: BlockInfo? = null
    }

    private class Message {
        val name: String? = null
        val html: String = ""
    }

    open class BlockInfo {

        @SerializedName("blockedbyid")
        val blockedById = 0
        @SerializedName("blockid")
        val blockId = 0
        @SerializedName("blockedby")
        val blockedBy: String = ""
        @SerializedName("blockreason")
        val blockReason: String = ""
        @SerializedName("blockedtimestamp")
        val blockTimeStamp: String = ""
        @SerializedName("blockexpiry")
        val blockExpiry: String = ""

        val isBlocked: Boolean
            get() {
                if (blockExpiry.isEmpty()) {
                    return false
                }
                val now = Date()
                val expiry = iso8601DateParse(blockExpiry)
                return expiry.after(now)
            }
    }
}
