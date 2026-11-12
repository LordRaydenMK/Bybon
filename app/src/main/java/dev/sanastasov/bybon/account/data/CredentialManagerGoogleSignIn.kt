package dev.sanastasov.bybon.account.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.account.googleWebClientId

class CredentialManagerGoogleSignIn(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) : GoogleSignIn {

    override suspend fun idToken(): String {
        val serverClientId = context.googleWebClientId()
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        try {
            val credential = credentialManager.getCredential(context, request).credential
            require(credential is CustomCredential)
            require(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GetCredentialException) {
            throw e.toSignInFailureOrRethrow()
        }
    }

    override suspend fun clearCredentialState() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

internal fun GetCredentialException.toSignInFailureOrRethrow(): SignInFailure = when (this) {
    is GetCredentialCancellationException -> SignInFailure.Canceled(this)

    is GetCredentialInterruptedException,
    is GetCredentialUnknownException,
    is NoCredentialException,
    -> SignInFailure.Failed(this)

    else -> throw this
}
