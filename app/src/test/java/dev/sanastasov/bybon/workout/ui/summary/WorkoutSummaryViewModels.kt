package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.workoutSummaryViewModel(
    sessionId: WorkoutSessionId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
    now: () -> LocalDateTime = { LocalDateTime.now() },
    defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
) = WorkoutSummaryViewModel(
    sessionId,
    repository,
    coroutineScope,
    now,
    defaultDispatcher,
)
