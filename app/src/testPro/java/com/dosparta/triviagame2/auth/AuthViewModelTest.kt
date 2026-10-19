package com.dosparta.triviagame2.auth

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private lateinit var viewModel: AuthViewModel
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        viewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `persisted session is exposed without an online login`() {
        repository.state.value = AuthState.SignedIn("existing", "Player")
        assertEquals(repository.state.value, viewModel.state.value)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun `duplicate sign in requests are ignored until completed`() = runTest {
        val finish = CompletableDeferred<Unit>()
        repository.onSignIn = { finish.await() }
        viewModel.signIn(context)
        viewModel.signIn(context)
        runCurrent()
        assertTrue(viewModel.busy.value)
        assertEquals(1, repository.signInCalls)
        finish.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.busy.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `successful sign in exposes authenticated user`() = runTest {
        repository.onSignIn = { repository.state.value = AuthState.SignedIn("new", "Player") }
        viewModel.signIn(context)
        advanceUntilIdle()
        assertEquals(AuthState.SignedIn("new", "Player"), viewModel.state.value)
    }

    @Test
    fun `cancelled account chooser stays signed out without an error`() = runTest {
        viewModel.signIn(context)
        advanceUntilIdle()
        assertEquals(AuthState.SignedOut, viewModel.state.value)
        assertFalse(viewModel.busy.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `network failure is visible and can be retried`() = runTest {
        repository.onSignIn = { throw AuthException(AuthError.NETWORK) }
        viewModel.signIn(context)
        advanceUntilIdle()
        assertEquals(AuthError.NETWORK, viewModel.error.value)
        repository.onSignIn = { repository.state.value = AuthState.SignedIn("retry", null) }
        viewModel.signIn(context)
        advanceUntilIdle()
        assertNull(viewModel.error.value)
        assertEquals(AuthState.SignedIn("retry", null), viewModel.state.value)
    }

    @Test
    fun `logout waits for game preservation and clears authentication only`() = runTest {
        repository.state.value = AuthState.SignedIn("first", null)
        val save = CompletableDeferred<Unit>()
        var preserved = false
        repository.onSignOut = { assertTrue(preserved) }
        viewModel.signOut { save.await(); preserved = true }
        runCurrent()
        assertTrue(viewModel.signingOut.value)
        assertEquals(0, repository.signOutCalls)
        save.complete(Unit)
        advanceUntilIdle()
        assertEquals(AuthState.SignedOut, viewModel.state.value)
        assertEquals(1, repository.signOutCalls)
        assertFalse(viewModel.signingOut.value)
    }

    @Test
    fun `credential session clearing failure does not undo Firebase logout`() = runTest {
        repository.state.value = AuthState.SignedIn("first", null)
        repository.onSignOut = { throw AuthException(AuthError.SESSION_CLEAR) }
        viewModel.signOut {}
        advanceUntilIdle()
        assertEquals(AuthState.SignedOut, viewModel.state.value)
        assertEquals(AuthError.SESSION_CLEAR, viewModel.error.value)
    }

    @Test
    fun `offline refresh does not discard persisted sign in`() = runTest {
        repository.state.value = AuthState.SignedIn("offline", null)
        repository.onRefresh = { throw AuthException(AuthError.NETWORK) }
        viewModel.refreshSession()
        advanceUntilIdle()
        assertEquals(AuthState.SignedIn("offline", null), viewModel.state.value)
        assertEquals(AuthError.NETWORK, viewModel.error.value)
    }

    @Test
    fun `successful refresh clears the previous network error after recovery`() = runTest {
        repository.state.value = AuthState.SignedIn("offline", null)
        repository.onRefresh = { throw AuthException(AuthError.NETWORK) }
        viewModel.refreshSession()
        advanceUntilIdle()
        assertEquals(AuthError.NETWORK, viewModel.error.value)

        val recovery = CompletableDeferred<Unit>()
        repository.onRefresh = { recovery.await() }
        viewModel.refreshSession()
        runCurrent()
        assertEquals(AuthError.NETWORK, viewModel.error.value)

        recovery.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.error.value)
        assertEquals(AuthState.SignedIn("offline", null), viewModel.state.value)
    }

    @Test
    fun `detected provider invalidation becomes signed out`() = runTest {
        repository.state.value = AuthState.SignedIn("revoked", null)
        repository.onRefresh = { repository.state.value = AuthState.SignedOut }
        viewModel.refreshSession()
        advanceUntilIdle()
        assertEquals(AuthState.SignedOut, viewModel.state.value)
    }
}

class FakeAuthRepository : AuthRepository {
    override val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    var signInCalls = 0
    var signOutCalls = 0
    var onSignIn: suspend () -> Unit = {}
    var onSignOut: suspend () -> Unit = {}
    var onRefresh: suspend () -> Unit = {}

    override suspend fun signIn(activityContext: Context) {
        signInCalls++
        onSignIn()
    }

    override suspend fun signOut() {
        signOutCalls++
        state.value = AuthState.SignedOut
        onSignOut()
    }

    override suspend fun refreshSession() = onRefresh()
}
