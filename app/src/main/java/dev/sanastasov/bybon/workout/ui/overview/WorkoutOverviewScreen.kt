package dev.sanastasov.bybon.workout.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dev.sanastasov.bybon.ui.components.NumberInputField
import dev.sanastasov.bybon.ui.components.rememberSyncedTextField
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import java.time.LocalDateTime
import java.util.Locale
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
                items(state.exercises, key = { it.id }) { exercise ->
                    Card(Modifier.fillMaxWidth()) {
                        OverviewExerciseCard(exercise, onAction)
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

@Composable
private fun OverviewExerciseCard(
    exercise: WorkoutExercise,
    onAction: (WorkoutOverviewAction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${exercise.sets.size} x ${exercise.exerciseDefinition.name} in ${exercise.repRange.first} - ${exercise.repRange.last}",
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
            )
            AdjustButtons(
                onDecrease = { onAction(WorkoutOverviewAction.OnDecreaseExercise(exercise)) },
                onIncrease = { onAction(WorkoutOverviewAction.OnIncreaseExercise(exercise)) },
                decreaseContentDescription = "Decrease weight and reps for ${exercise.exerciseDefinition.name}",
                increaseContentDescription = "Increase weight and reps for ${exercise.exerciseDefinition.name}",
            )
        }
        Spacer(Modifier.height(4.dp))

        exercise.sets.forEachIndexed { index, set ->
            val weightState = rememberSyncedTextField(
                key = exercise,
                initialText = remember(exercise) { set.weight.kilograms },
            ) { weight ->
                onAction(WorkoutOverviewAction.OnWeightUpdated(weight, exercise, index))
            }
            val repState = rememberSyncedTextField(
                key = exercise,
                initialText = remember(exercise) { set.reps.toString() },
            ) { reps ->
                onAction(WorkoutOverviewAction.OnRepsUpdated(reps, exercise, index))
            }

            OverviewSetRow(
                index = index,
                set = set,
                weightState = weightState,
                repState = repState,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton({ onAction(WorkoutOverviewAction.OnAddSet(exercise)) }) {
                Text("Add set")
            }

            if (exercise.sets.size > 1) {
                TextButton({ onAction(WorkoutOverviewAction.RemoveLastSet(exercise)) }) {
                    Text("Remove last set")
                }
            }
        }
    }
}

@Composable
private fun OverviewSetRow(
    index: Int,
    set: ExerciseSet,
    weightState: TextFieldState,
    repState: TextFieldState,
) {
    val oneRmLabel = set.oneRm?.let { "@ ${formatOneRmKg(it)} kg 1RM" }
    val previousLabel = set.previous?.let { previous ->
        buildString {
            append("${previous.weight.kilograms} kg x ${previous.reps}")
            previous.oneRm?.let { append(" @ ${formatOneRmKg(it)} kg 1RM") }
        }
    }
    val setDescription = buildString {
        append("Set ${index + 1}, ${set.weight.kilograms} kg by ${set.reps}")
        if (oneRmLabel != null) {
            append(", $oneRmLabel")
        }
        if (previousLabel != null) {
            append(". Previous: $previousLabel")
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = setDescription
            },
    ) {
        Row(
            Modifier
                .defaultMinSize(minHeight = 48.dp)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${index + 1}.")
                    NumberInputField(weightState)
                    Text(" kg x ")
                    NumberInputField(repState)
                    if (oneRmLabel != null) {
                        Text(oneRmLabel)
                    }
                }
                if (previousLabel != null) {
                    Text(
                        previousLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustButtons(
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseContentDescription: String,
    increaseContentDescription: String,
) {
    Row {
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

private fun formatOneRmKg(kg: Float): String =
    "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')

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
                )
            },
            state = WorkoutState.Completed(
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                duration = Duration.ZERO,
            ),
        )
    }
    OverviewScreenContent(fullBodyA.toOverviewSession(previous), {}, {})
}
