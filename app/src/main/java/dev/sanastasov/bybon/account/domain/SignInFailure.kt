package dev.sanastasov.bybon.account.domain

sealed class SignInFailure(
    cause: Throwable? = null,
) : Exception(cause) {
    class Canceled(
        cause: Throwable? = null,
    ) : SignInFailure(cause)

    class Failed(
        cause: Throwable? = null,
    ) : SignInFailure(cause)
}
