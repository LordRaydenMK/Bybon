package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.RestPromoteScheduler
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope

internal fun TestScope.sessionViewModel(
    planId: WorkoutPlanId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
    restPromoteScheduler: RestPromoteScheduler = RestPromoteScheduler.NoOp,
    defaultDispatcher: CoroutineDispatcher =
        coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
) = WorkoutSessionViewModel(
    planId,
    repository,
    coroutineScope,
    restPromoteScheduler,
    defaultDispatcher,
)
