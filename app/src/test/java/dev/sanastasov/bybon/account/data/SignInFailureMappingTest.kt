package dev.sanastasov.bybon.account.data

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import dev.sanastasov.bybon.account.domain.SignInFailure
import kotlin.test.assertFailsWith
import org.junit.Test

class SignInFailureMappingTest {

    @Test
    fun `canceled google credential maps with cause`() {
        val original = GetCredentialCancellationException("canceled")
        val mapped = original.toSignInFailureOrRethrow()
        assert(mapped is SignInFailure.Canceled)
        assert(mapped.cause === original)
    }

    @Test
    fun `handleable google credential failures map with cause`() {
        listOf(
            GetCredentialInterruptedException("interrupted"),
            GetCredentialUnknownException("unknown"),
            NoCredentialException("none"),
        ).forEach { original ->
            val mapped = original.toSignInFailureOrRethrow()
            assert(mapped is SignInFailure.Failed)
            assert(mapped.cause === original)
        }
    }

    @Test
    fun `google credential misconfiguration is not mapped`() {
        listOf(
            GetCredentialProviderConfigurationException("provider"),
            GetCredentialUnsupportedException("unsupported"),
        ).forEach { original ->
            val thrown = assertFailsWith<Exception> { original.toSignInFailureOrRethrow() }
            assert(thrown === original)
        }
    }
}
