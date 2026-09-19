package dev.sanastasov.bybon.workout.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.ui.ExerciseCard
import dev.sanastasov.bybon.workout.ui.ExerciseCardEvent
import dev.sanastasov.bybon.workout.ui.ExerciseCardMode
import java.time.LocalDateTime
import kotlin.time.Duration

@Composable
fun WorkoutModule.WorkoutOverviewScreen(
    planId: WorkoutPlanId,
    onBack: () -> Unit,
    onStartSession: () -> Unit,
) {
    val viewModel = retain {
        WorkoutOverviewViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            WorkoutOverviewEffect.NavigateToSession -> onStartSession()
        }
    }
    uiState?.let {
        OverviewScreenContent(it, viewModel::onAction, onBack)
    }
}

@Composable
private fun OverviewScreenContent(
    state: WorkoutSession,
    onAction: (WorkoutOverviewAction) -> Unit,
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
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.planName, fontWeight = FontWeight.Bold)
                    state.planDescription?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it)
                    }
                }
                AdjustButtons(
                    onDecrease = { onAction(WorkoutOverviewAction.OnDecreaseWorkout) },
                    onIncrease = { onAction(WorkoutOverviewAction.OnIncreaseWorkout) },
                    decreaseContentDescription = "Decrease weight and reps for all exercises",
                    increaseContentDescription = "Increase weight and reps for all exercises",
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    state.exercises,
                    key = { _, exercise -> exercise.id },
                ) { index, exercise ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        ExerciseCard(
                            exercise = exercise,
                            mode = ExerciseCardMode.Overview,
                            onEvent = { event -> onAction(event.toOverviewAction(exercise)) },
                            overflow = state.exerciseOverflow(index, onAction),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                { onAction(WorkoutOverviewAction.OnStartWorkout) },
                Modifier.fillMaxWidth(),
            ) {
                Text("Start Workout")
            }
        }
    }
}

private fun ExerciseCardEvent.toOverviewAction(exercise: WorkoutExercise): WorkoutOverviewAction =
    when (this) {
        ExerciseCardEvent.OnIncrease -> WorkoutOverviewAction.OnIncreaseExercise(exercise)

        ExerciseCardEvent.OnDecrease -> WorkoutOverviewAction.OnDecreaseExercise(exercise)

        ExerciseCardEvent.OnResetAllSets -> WorkoutOverviewAction.OnResetExercise(exercise)

        is ExerciseCardEvent.OnResetSet ->
            WorkoutOverviewAction.OnResetSet(exercise, index, isWarmup)

        is ExerciseCardEvent.OnWeightUpdated ->
            WorkoutOverviewAction.OnWeightUpdated(weight, exercise, index, isWarmup)

        is ExerciseCardEvent.OnRepsUpdated ->
            WorkoutOverviewAction.OnRepsUpdated(reps, exercise, index, isWarmup)

        ExerciseCardEvent.OnAddSet -> WorkoutOverviewAction.OnAddSet(exercise)

        ExerciseCardEvent.OnRemoveLastSet -> WorkoutOverviewAction.RemoveLastSet(exercise)

        ExerciseCardEvent.OnConvertToWarmup -> WorkoutOverviewAction.OnConvertToWarmup(exercise)

        ExerciseCardEvent.OnConvertToWorkSet -> WorkoutOverviewAction.OnConvertToWorkSet(exercise)

        is ExerciseCardEvent.OnCompleteSet -> error("Complete set is not supported in overview")
    }

@Composable
private fun AdjustButtons(
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseContentDescription: String,
    increaseContentDescription: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onDecrease,
            Modifier.semantics { contentDescription = decreaseContentDescription },
        ) {
            Text("−", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(
            onIncrease,
            Modifier.semantics { contentDescription = increaseContentDescription },
        ) {
            Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Preview
@Composable
private fun OverviewScreenContentPreview() {
    val previous = fullBodyA.toOverviewSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        set.copy(
                            weight = Weight.kilograms(45),
                            reps = 9,
                            setState = SetState.Completed,
                            previous = PreviousSetPerformance(Weight.kilograms(45), 9),
                        )
                    },
                    warmupSets = exercise.warmupSets?.map { set ->
                        set.copy(setState = SetState.Completed)
                    },
                )
            },
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            state = WorkoutState.Completed(Duration.ZERO),
        )
    }
    OverviewScreenContent(fullBodyA.toOverviewSession(previous), {}, {})
}
