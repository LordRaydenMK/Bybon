package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.domain.OpenWorkoutSnapshot
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.startWorkout
import dev.sanastasov.bybon.workout.domain.toOpenSnapshot
import dev.sanastasov.bybon.workout.domain.updateWorkout
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

interface WorkoutSessionRuntime {
    val state: StateFlow<OpenWorkoutSnapshot?>
}

class WorkoutSessionRuntimeImpl(
    repository: WorkoutsRepository,
    scope: CoroutineScope,
    now: () -> LocalDateTime = LocalDateTime::now,
) : WorkoutSessionRuntime {
    override val state: StateFlow<OpenWorkoutSnapshot?> =
        combine(repository.workoutSessions(), ticksEverySecond()) { sessions, _ ->
            sessions.firstNotNullOfOrNull { it.toOpenSnapshot(now()) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        state
            .map { snapshot ->
                snapshot?.takeIf { it.shouldActivateNextSet }?.session?.planId
            }
            .distinctUntilChanged()
            .onEach { planId ->
                if (planId != null) {
                    repository.updateWorkout(planId) { it.startWorkout(now()) }
                }
            }
            .launchIn(scope)
    }
}
