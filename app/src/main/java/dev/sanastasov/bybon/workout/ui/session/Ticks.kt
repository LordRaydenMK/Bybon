package dev.sanastasov.bybon.workout.ui.session

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile

internal fun ticksEverySecond(): Flow<Long> = flow {
    var tick = 0L
    while (true) {
        emit(tick)
        delay(1.seconds)
        tick++
    }
}

internal fun <T> Flow<T>.takeUntil(predicate: (T) -> Boolean): Flow<T> = transformWhile { value ->
    emit(value)
    !predicate(value)
}
