package com.dosparta.trivia.domain.model

/**
 * Languages the app ships UI strings for and can translate trivia content into.
 *
 * OpenTDB only serves English content, so [ENGLISH] is the source language and needs no translation.
 *
 * @property tag BCP-47 language tag used for the per-app locale and the translation target
 */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    GERMAN("de"),
    SPANISH("es"),
    CHINESE_SIMPLIFIED("zh-CN");

    val requiresTranslation: Boolean
        get() = this != ENGLISH

    companion object {
        val DEFAULT = ENGLISH

        /** Resolves a BCP-47 tag (e.g. `de-AT`, `zh-Hans-CN`) to a supported language, or null. */
        fun fromTag(tag: String?): AppLanguage? {
            val parts = tag?.trim()?.replace('_', '-')?.split('-')?.filter { it.isNotEmpty() }
                ?.map { it.lowercase() }
                ?: return null
            return when (parts.firstOrNull()) {
                "en" -> ENGLISH
                "de" -> GERMAN
                "es" -> SPANISH
                "zh" -> if (isTraditionalChinese(parts.drop(1))) null else CHINESE_SIMPLIFIED
                else -> null
            }
        }

        private fun isTraditionalChinese(subtags: List<String>): Boolean {
            if ("hans" in subtags) return false
            return "hant" in subtags || subtags.any { it in TRADITIONAL_CHINESE_REGIONS }
        }

        private val TRADITIONAL_CHINESE_REGIONS = setOf("tw", "hk", "mo")
    }
}
