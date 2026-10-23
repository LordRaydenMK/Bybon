package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class ExerciseDetailsViewModel(
    private val exerciseId: String,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
    now: () -> LocalDateTime = { LocalDateTime.now() },
) {

    val uiState: StateFlow<ExerciseDetailsUiState> =
        combine(repository.exercises(), repository.workoutSessions()) { exercises, sessions ->
            val exercise = checkNotNull(exercises.firstOrNull { it.id == exerciseId }) {
                "Exercise $exerciseId is not in the repository"
            }
            val completed = sessions.filter { it.state is WorkoutState.Completed }
            exercise.toDetailsUi(completed.exercisePerformance(exerciseId, now()))
        }.stateInWhileInForeground(coroutineScope, ExerciseDetailsUiState.Loading)
}
