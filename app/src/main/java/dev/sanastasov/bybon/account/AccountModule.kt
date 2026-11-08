package dev.sanastasov.bybon.account

import dev.sanastasov.bybon.BuildConfig
import dev.sanastasov.bybon.account.data.FirebaseAccountAuth
import dev.sanastasov.bybon.account.domain.AccountAuth

interface AccountModule {
    val accountAuth: AccountAuth
    val firebaseEnvName: String

    companion object {
        fun create(): AccountModule = object : AccountModule {
            override val accountAuth: AccountAuth = FirebaseAccountAuth()
            override val firebaseEnvName: String = BuildConfig.FIREBASE_ENV
        }
    }
}
