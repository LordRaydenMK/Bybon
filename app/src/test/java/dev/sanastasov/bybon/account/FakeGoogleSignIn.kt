package dev.sanastasov.bybon.account

import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure

class FakeGoogleSignIn(
    var result: Result<String> = Result.success("token"),
) : GoogleSignIn {

    var cleared = false
        private set

    override suspend fun idToken(): String = result.getOrElse { throw it }

    override suspend fun clearCredentialState() {
        cleared = true
    }

    companion object {
        fun canceled(): FakeGoogleSignIn = FakeGoogleSignIn(Result.failure(SignInFailure.Canceled))
        fun failed(): FakeGoogleSignIn = FakeGoogleSignIn(Result.failure(SignInFailure.Failed))
    }
}
