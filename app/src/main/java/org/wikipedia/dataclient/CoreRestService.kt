package org.wikipedia.dataclient

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.restbase.EditCount
import retrofit2.http.GET
import retrofit2.http.Path

interface CoreRestService {

    @GET("revision/{oldRev}/compare/{newRev}")
    fun getDiff(
        @Path("oldRev") oldRev: Long,
        @Path("newRev") newRev: Long
    ): Observable<DiffResponse>

    @GET("page/{title}/history/counts/{editType}")
    suspend fun getEditCount(
            @Path("title") title: String,
            @Path("editType") editType: String
    ): EditCount

    companion object {
        const val CORE_REST_API_PREFIX = "w/rest.php/v1/"
    }
}
