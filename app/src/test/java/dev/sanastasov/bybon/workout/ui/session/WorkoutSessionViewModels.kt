package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.RestPromoteScheduler
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.sessionViewModel(
    planId: WorkoutPlanId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
    restPromoteScheduler: RestPromoteScheduler = RestPromoteScheduler.NoOp,
    defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
) = WorkoutSessionViewModel(
    planId,
    repository,
    coroutineScope,
    restPromoteScheduler,
    defaultDispatcher,
)
