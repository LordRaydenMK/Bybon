package dev.sanastasov.bybon.workout

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal fun ticksEverySecond(): Flow<Long> = flow {
    var tick = 0L
    while (true) {
        emit(tick)
        delay(1.seconds)
        tick++
    }
}
