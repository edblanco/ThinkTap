package com.dosparta.trivia.data.localization.deepl

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = false)
internal data class DeepLTranslationRequest(
    @param:Json(name = "text")
    val text: List<String>,

    @param:Json(name = "target_lang")
    val targetLang: String,

    @param:Json(name = "source_lang")
    val sourceLang: String = "EN"
)

@JsonClass(generateAdapter = false)
internal data class DeepLTranslationResponse(
    @param:Json(name = "translations")
    val translations: List<DeepLTranslationItem> = emptyList()
)

@JsonClass(generateAdapter = false)
internal data class DeepLTranslationItem(
    @param:Json(name = "detected_source_language")
    val detectedSourceLanguage: String? = null,

    @param:Json(name = "text")
    val text: String
)

internal interface DeepLApi {
    @POST("translate")
    suspend fun translate(
        @Header("Authorization") authorization: String,
        @Body request: DeepLTranslationRequest
    ): DeepLTranslationResponse
}
