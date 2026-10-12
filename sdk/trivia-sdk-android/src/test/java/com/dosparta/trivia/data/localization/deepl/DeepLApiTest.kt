package com.dosparta.trivia.data.localization.deepl

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.POST
import java.net.URI

class DeepLApiTest {
    @Test
    fun `translation route resolves under the versioned base URL`() {
        val route = DeepLApi::class.java.declaredMethods
            .single { it.name == "translate" }
            .getAnnotation(POST::class.java)!!.value

        for (host in listOf("api-free.deepl.com", "api.deepl.com")) {
            assertEquals(
                "https://$host/v2/translate",
                URI("https://$host/v2/").resolve(route).toString()
            )
        }
    }
}
