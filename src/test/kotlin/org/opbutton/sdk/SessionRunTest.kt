package org.opbutton.sdk

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class SessionRunTest {
    @Test fun retriesAndApprovalPollKeepRunIdentityButNextSdkGetsAnother() {
        val requests = mutableListOf<Request>()
        val runs = mutableListOf<String>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                runs += JSONObject(buffer.readUtf8()).getString("run_id")
            }
            if (requests.size == 1) throw IOException("Connection dropped after request")
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"request_id":"request","status":"pending"}""".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val first = OpButtonHttpClient("https://opbutton.invalid", "test", transport)
        val next = OpButtonHttpClient("https://opbutton.invalid", "test", transport)
        try {
            try {
                first.connect("phone", "Phone", emptyMap())
                fail("Expected simulated network failure")
            } catch (expected: OpButtonException) { assertTrue(expected.isNetworkError) }
            first.connect("phone", "Phone", emptyMap())
            first.pollStatus("request")
            next.connect("phone", "Phone", emptyMap())
            assertEquals(runs[0], runs[1])
            assertEquals(runs[0], requests[2].url.queryParameter("run_id"))
            assertNotEquals(runs[0], runs[2])
            runs.forEach { assertEquals(it, UUID.fromString(it).toString()) }
            requests.forEach { assertEquals("test", it.header("X-Api-Token")) }
        } finally { first.shutdown(); next.shutdown() }
    }
}
