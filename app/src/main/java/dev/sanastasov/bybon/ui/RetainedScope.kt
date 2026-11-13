package dev.sanastasov.bybon.ui

import dev.marcellogalhardo.retained.core.RetainedEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Retained viewModelScope on [Dispatchers.Default]. Cancelled when the entry is cleared. */
fun RetainedEntry.defaultScope(): CoroutineScope =
    CoroutineScope(coroutineScope.coroutineContext + Dispatchers.Default)
