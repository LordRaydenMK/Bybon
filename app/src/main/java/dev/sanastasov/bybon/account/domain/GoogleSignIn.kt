package dev.sanastasov.bybon.account.domain

interface GoogleSignIn {
    suspend fun idToken(): String
    suspend fun clearCredentialState()
}

sealed class SignInFailure : Exception() {
    data object Canceled : SignInFailure()
    data object Failed : SignInFailure()
}
