package dev.sanastasov.bybon.account

import dev.sanastasov.bybon.account.domain.AccountAuth
import dev.sanastasov.bybon.account.domain.AccountUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountAuth(
    initial: AccountUser? = null,
) : AccountAuth {

    private val current = MutableStateFlow(initial)
    var signedOut = false
        private set

    override fun user(): Flow<AccountUser?> = current

    override suspend fun signInWithIdToken(idToken: String) {
        current.value = AccountUser("$idToken@example.com")
    }

    override suspend fun signOut() {
        signedOut = true
        current.value = null
    }

    fun setUser(user: AccountUser?) {
        current.value = user
    }
}
