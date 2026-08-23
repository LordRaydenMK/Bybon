package dev.sanastasov.bybon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <A> Flow<A>.stateInWhileInForeground(
    scope: CoroutineScope,
    initialValue: A,
) = stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialValue)

@Composable
fun <A> Flow<A>.collectEffectWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
    action: (A) -> Unit
) = LaunchedEffect(Unit) {
    flowWithLifecycle(lifecycleOwner.lifecycle)
        .collect(action)
}
