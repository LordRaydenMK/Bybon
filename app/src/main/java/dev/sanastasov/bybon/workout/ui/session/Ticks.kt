package dev.sanastasov.bybon.workout.ui.session

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal fun ticksEverySecond(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1.seconds)
    }
}
