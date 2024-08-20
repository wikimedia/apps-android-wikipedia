package org.wikipedia.login

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class UserExtendedInfoClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("user_extended_info.json")
        val id = 24531888
        runBlocking {
            val userInfo = apiService.getUserInfo()
            assert(userInfo.query?.userInfo?.id == id)
            assert(userInfo.query?.getUserResponse("USER")?.name == "USER")
        }
    }
}
