package dev.sanastasov.bybon.account.domain

interface GoogleSignIn {
    suspend fun idToken(): String
    suspend fun clearCredentialState()
}
