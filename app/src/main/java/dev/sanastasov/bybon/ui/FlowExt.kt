package dev.sanastasov.bybon.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

fun <A> Flow<A>.stateInWhileInForeground(
    scope: CoroutineScope,
    initialValue: A,
) = stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialValue)