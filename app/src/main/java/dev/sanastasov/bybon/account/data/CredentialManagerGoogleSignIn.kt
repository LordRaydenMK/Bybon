package dev.sanastasov.bybon.account.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure
import kotlin.coroutines.cancellation.CancellationException

class CredentialManagerGoogleSignIn(
    private val context: Context,
    private val serverClientId: String,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) : GoogleSignIn {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun idToken(): String = try {
        requestIdToken()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw mapToSignInFailure(e)
    }

    private suspend fun requestIdToken(): String {
        check(serverClientId.isNotBlank())
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val credential = credentialManager.getCredential(context, request).credential
        val googleIdToken = credential as? CustomCredential
        check(googleIdToken != null)
        check(googleIdToken.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        return GoogleIdTokenCredential.createFrom(googleIdToken.data).idToken
    }

    private fun mapToSignInFailure(e: Exception): SignInFailure = when (e) {
        is SignInFailure -> e
        is GetCredentialCancellationException -> SignInFailure.Canceled
        else -> SignInFailure.Failed
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun clearCredentialState() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Firebase session already ended.
        }
    }
}
