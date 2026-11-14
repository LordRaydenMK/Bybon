package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope

internal fun TestScope.workoutSummaryViewModel(
    sessionId: WorkoutSessionId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
    now: () -> LocalDateTime = { LocalDateTime.now() },
) = WorkoutSummaryViewModel(
    sessionId,
    repository,
    coroutineScope,
    now,
)
