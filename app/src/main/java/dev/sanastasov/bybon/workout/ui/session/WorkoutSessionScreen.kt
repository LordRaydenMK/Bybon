package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.ui.ExerciseCard
import dev.sanastasov.bybon.workout.ui.ExerciseCardEvent
import dev.sanastasov.bybon.workout.ui.ExerciseCardMode
import java.time.LocalDateTime
import kotlin.time.Duration

@Composable
fun WorkoutModule.WorkoutSessionScreen(planId: WorkoutPlanId, onBack: () -> Unit) {
    val viewModel = retain {
        WorkoutSessionViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState?.let {
        SessionScreenContent(
            it,
            viewModel::onAction,
            onBack,
        )
    }
}

@Composable
private fun SessionScreenContent(
    state: WorkoutSession,
    onAction: (WorkoutSessionAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { BybonTopAppBar(state.planName, onBack) },
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxSize(),
        ) {
            state.planDescription?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }
            val pagerState = rememberPagerState(0) {
                state.exercises.size
            }
            HorizontalPager(
                pagerState,
                Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val exercise = state.exercises[page]
                Card(Modifier.fillMaxWidth()) {
                    ExerciseCard(
                        exercise = exercise,
                        mode = ExerciseCardMode.Session,
                        onEvent = { event -> onAction(event.toSessionAction(exercise)) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Exercise ${pagerState.currentPage + 1} / ${state.exercises.size}")
        }
    }
}

private fun ExerciseCardEvent.toSessionAction(exercise: WorkoutExercise): WorkoutSessionAction =
    when (this) {
        ExerciseCardEvent.OnIncrease -> WorkoutSessionAction.OnIncreaseExercise(exercise)

        ExerciseCardEvent.OnDecrease -> WorkoutSessionAction.OnDecreaseExercise(exercise)

        ExerciseCardEvent.OnResetAllSets -> WorkoutSessionAction.OnResetExercise(exercise)

        is ExerciseCardEvent.OnResetSet ->
            WorkoutSessionAction.OnResetSet(exercise, index, isWarmup)

        is ExerciseCardEvent.OnWeightUpdated ->
            WorkoutSessionAction.OnWeightUpdated(weight, exercise, index, isWarmup)

        is ExerciseCardEvent.OnRepsUpdated ->
            WorkoutSessionAction.OnRepsUpdated(reps, exercise, index, isWarmup)

        ExerciseCardEvent.OnAddSet -> WorkoutSessionAction.OnAddSet(exercise)

        ExerciseCardEvent.OnRemoveLastSet -> WorkoutSessionAction.RemoveLastSet(exercise)

        ExerciseCardEvent.OnConvertToWarmup -> WorkoutSessionAction.OnConvertToWarmup(exercise)

        ExerciseCardEvent.OnConvertToWorkSet -> WorkoutSessionAction.OnConvertToWorkSet(exercise)

        is ExerciseCardEvent.OnCompleteSet ->
            WorkoutSessionAction.OnCompleteSet(exercise, index, isWarmup)
    }

@Preview
@Composable
private fun SessionScreenContentPage1CompletedExercisePreview() {
    val session = fullBodyA.toWorkoutSession()
    SessionScreenContent(
        session.completeSet(session.exercises.first(), 0, isWarmup = true),
        {},
        {},
    )
}

@Preview
@Composable
private fun SessionScreenContentInitialPreview() {
    SessionScreenContent(fullBodyA.toWorkoutSession(), {}, {})
}

@Preview
@Composable
private fun SessionScreenContentWithPreviousPreview() {
    val previous = fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(40 + index),
                            reps = 9,
                            setState = SetState.Completed,
                        )
                    },
                    warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(22 + index),
                            reps = 6,
                            setState = SetState.Completed,
                        )
                    },
                )
            },
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            state = WorkoutState.Completed(Duration.ZERO),
        )
    }
    SessionScreenContent(fullBodyA.toWorkoutSession(previous), {}, {})
}
