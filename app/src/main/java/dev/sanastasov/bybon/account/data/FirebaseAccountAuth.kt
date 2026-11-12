package dev.sanastasov.bybon.account.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dev.sanastasov.bybon.account.domain.AccountAuth
import dev.sanastasov.bybon.account.domain.AccountUser
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.account.domain.UserId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAccountAuth(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AccountAuth {

    override fun user(): Flow<AccountUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAccountUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithIdToken(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
        } catch (e: FirebaseException) {
            throw e.toSignInFailureOrRethrow()
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

internal fun FirebaseException.toSignInFailureOrRethrow(): SignInFailure = when (this) {
    is FirebaseAuthException,
    is FirebaseNetworkException,
    is FirebaseTooManyRequestsException,
    -> SignInFailure.Failed(this)

    else -> throw this
}

private fun FirebaseUser.toAccountUser(): AccountUser = AccountUser(
    uid = UserId(uid),
    email = requireNotNull(email) { "Signed-in user has no email" },
)
