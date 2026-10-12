package com.dosparta.trivia.sdk.android

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.dosparta.core.network.NetworkClientFactory
import com.dosparta.trivia.data.BuildConfig
import com.dosparta.trivia.data.local.database.TriviaDatabase
import com.dosparta.trivia.data.localization.ContentLocalizationRepositoryImpl
import com.dosparta.trivia.data.localization.DualEngineTextTranslator
import com.dosparta.trivia.data.localization.MlKitTextTranslator
import com.dosparta.trivia.data.localization.TriviaContentTranslator
import com.dosparta.trivia.data.localization.deepl.DeepLApi
import com.dosparta.trivia.data.localization.deepl.DeepLTextTranslator
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.persistenceCall
import com.dosparta.trivia.data.repository.GameSessionRepositoryImpl
import com.dosparta.trivia.data.repository.TranslatingTriviaRepository
import com.dosparta.trivia.data.repository.TriviaRepositoryImpl
import com.dosparta.trivia.data.token.SessionTokenStore
import com.dosparta.trivia.data.token.TriviaSessionTokenProviderImpl
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.sdk.TriviaSdkException
import kotlinx.coroutines.Dispatchers

/** Android wiring that requires neither Hilt nor an application-specific dependency graph. */
object AndroidTriviaSdk {
    private var resources: Resources? = null

    /**
     * Returns a fresh game controller backed by application-scoped adapters.
     *
     * Only [Context.getApplicationContext] is retained. Room, networking, the token provider,
     * localization state and ML Kit clients are shared for the application process lifetime.
     * A language change is therefore visible to every controller. Controllers do not
     * own these resources and must not close them. There is no factory-owned coroutine scope.
     * Persistence retains the existing single active game session.
     */
    @JvmStatic
    fun create(context: Context): TriviaSdk {
        val applicationContext = context.applicationContext
            ?: throw TriviaSdkException(TriviaError.INVALID_CONFIGURATION)
        val shared = synchronized(this) {
            resources ?: Resources(applicationContext).also { resources = it }
        }
        return TriviaSdk(
            repository = shared.repository,
            sessions = shared.sessions,
            localization = shared.localization
        )
    }

    private class Resources(context: Context) {
        init {
            Log.d(
                "AndroidTriviaSdk",
                "DeepL host=${java.net.URI(BuildConfig.DEEPL_BASE_URL).host}; " +
                    "API key configured=${BuildConfig.DEEPL_API_KEY.isNotBlank()}."
            )
        }

        private val api = NetworkClientFactory.create().create(TriviaApi::class.java)
        private val database = persistenceCall {
            Room.databaseBuilder(context, TriviaDatabase::class.java, "trivia_database").build()
        }
        private val tokens = TriviaSessionTokenProviderImpl(
            api,
            persistenceCall { SessionTokenStore(context) }
        )
        val localization = ContentLocalizationRepositoryImpl()
        private val deepLApi = NetworkClientFactory.create(BuildConfig.DEEPL_BASE_URL).create(DeepLApi::class.java)
        private val deepLTranslator = DeepLTextTranslator(deepLApi, BuildConfig.DEEPL_API_KEY)
        private val mlKitTranslator = MlKitTextTranslator()
        private val translator = DualEngineTextTranslator(
            primary = deepLTranslator,
            fallback = mlKitTranslator
        )
        val repository = TranslatingTriviaRepository(
            TriviaRepositoryImpl(api, tokens, Dispatchers.IO),
            TriviaContentTranslator(translator),
            localization
        )
        val sessions = GameSessionRepositoryImpl(
            persistenceCall { database.gameSessionDao() },
            Dispatchers.IO
        )
    }
}
