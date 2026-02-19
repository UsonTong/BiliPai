package com.android.purebilibili.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import retrofit2.http.Body
import retrofit2.http.Query

class DynamicApiContractTest {

    @Test
    fun likeDynamic_usesQueryCsrfAndJsonBody() {
        val method = DynamicApi::class.java.methods.first { it.name == "likeDynamic" }

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        val secondParamAnnotations = method.parameterAnnotations[1].toList()
        val thirdParamAnnotations = method.parameterAnnotations[2].toList()

        val query = firstParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("csrf", query?.value)

        val csrfTokenQuery = secondParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("csrf_token", csrfTokenQuery?.value)

        assertTrue(thirdParamAnnotations.any { it is Body })
        assertEquals(DynamicThumbRequest::class.java, method.parameterTypes[2])
    }

    @Test
    fun dynamicThumbRequest_defaultsMatchDesktopWebClient() {
        val request = DynamicThumbRequest(dyn_id_str = "123", up = 1)

        assertEquals("333.1369.0.0", request.spmid)
        assertEquals("333.999.0.0", request.from_spmid)
    }
}
