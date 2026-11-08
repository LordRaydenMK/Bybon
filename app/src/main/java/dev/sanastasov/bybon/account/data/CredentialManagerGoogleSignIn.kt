package dev.sanastasov.bybon.account.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.account.googleWebClientId

class CredentialManagerGoogleSignIn(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) : GoogleSignIn {

    override suspend fun idToken(): String {
        val serverClientId = context.googleWebClientId()
        try {
            val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val credential = credentialManager.getCredential(context, request).credential
            require(credential is CustomCredential)
            require(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GetCredentialException) {
            throw if (e is GetCredentialCancellationException) {
                SignInFailure.Canceled
            } else {
                SignInFailure.Failed
            }
        } catch (_: GoogleIdTokenParsingException) {
            throw SignInFailure.Failed
        }
    }

    override suspend fun clearCredentialState() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
