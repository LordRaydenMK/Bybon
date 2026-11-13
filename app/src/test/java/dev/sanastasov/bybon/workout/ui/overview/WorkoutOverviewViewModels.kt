package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.overviewViewModel(
    planId: WorkoutPlanId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
    defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
) = WorkoutOverviewViewModel(
    planId,
    repository,
    coroutineScope,
    defaultDispatcher,
)
