package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope

internal fun TestScope.overviewViewModel(
    planId: WorkoutPlanId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
) = WorkoutOverviewViewModel(
    planId,
    repository,
    coroutineScope,
)
