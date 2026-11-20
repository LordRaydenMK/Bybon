package dev.sanastasov.bybon.account

import dev.sanastasov.bybon.account.data.FirebaseAccountAuth
import dev.sanastasov.bybon.account.domain.AccountAuth

interface AccountModule {
    val accountAuth: AccountAuth

    companion object {
        fun create(): AccountModule = object : AccountModule {
            override val accountAuth: AccountAuth = FirebaseAccountAuth()
        }
    }
}
