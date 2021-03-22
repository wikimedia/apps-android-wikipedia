package org.wikipedia.edit

import org.wikipedia.dataclient.mwapi.MwPostResponse

class Edit : MwPostResponse() {
    private val edit: Result? = null
    fun edit(): Result? {
        return edit
    }

    fun hasEditResult(): Boolean {
        return edit != null
    }

    class Result {
        private val result: String? = null
        private val newrevid: Long = 0
        private val captcha: Captcha? = null
        private val code: String? = null
        private val info: String? = null
        private val warning: String? = null
        private val spamblacklist: String? = null
        fun status(): String? {
            return result
        }

        fun newRevId(): Long {
            return newrevid
        }

        fun editSucceeded(): Boolean {
            return "Success" == result
        }

        fun captchaId(): String? {
            return captcha?.id()
        }

        fun hasEditErrorCode(): Boolean {
            return code != null
        }

        fun hasCaptchaResponse(): Boolean {
            return captcha != null
        }

        fun code(): String? {
            return code
        }

        fun info(): String? {
            return info
        }

        fun warning(): String? {
            return warning
        }

        fun spamblacklist(): String? {
            return spamblacklist
        }

        fun hasSpamBlacklistResponse(): Boolean {
            return spamblacklist != null
        }
    }

    private class Captcha {
        private val id: String? = null
        fun id(): String? {
            return id
        }
    }
}
