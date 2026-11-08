package dev.sanastasov.bybon.account.domain

import kotlinx.coroutines.flow.Flow

interface AccountAuth {
    fun user(): Flow<AccountUser?>
    suspend fun signInWithIdToken(idToken: String)
    suspend fun signOut()
}
