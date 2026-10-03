package com.dosparta.trivia.data.token

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SessionTokenStore @Inject constructor(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getToken(): String? = preferences.getString(KEY_TOKEN, null)

    fun getLastUsedAtMillis(): Long = preferences.getLong(KEY_LAST_USED_AT_MILLIS, 0L)

    fun saveToken(token: String, lastUsedAtMillis: Long) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_LAST_USED_AT_MILLIS, lastUsedAtMillis)
            .apply()
    }

    fun updateLastUsedAt(lastUsedAtMillis: Long) {
        preferences.edit()
            .putLong(KEY_LAST_USED_AT_MILLIS, lastUsedAtMillis)
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_LAST_USED_AT_MILLIS)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "opentdb_token_store"
        const val KEY_TOKEN = "token"
        const val KEY_LAST_USED_AT_MILLIS = "last_used_at_millis"
    }
}
