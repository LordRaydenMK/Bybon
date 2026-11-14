package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope

internal fun TestScope.plansViewModel(
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope = backgroundScope,
) = WorkoutPlansViewModel(
    repository,
    coroutineScope,
)
