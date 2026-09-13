package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun WorkoutModule.WorkoutSessionScreen(planId: WorkoutPlanId) {
    val viewModel = retain {
        WorkoutSessionViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState?.let {
        SessionScreenContent(
            it,
            viewModel::onAction
        )
    }
}

@Composable
private fun SessionScreenContent(
    state: WorkoutSession,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    Scaffold(
        topBar = { BybonTopAppBar(state.planName, {}) }
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            state.planDescription?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }
            val pagerState = rememberPagerState(0) {
                state.exercises.size
            }
            HorizontalPager(pagerState) { page ->
                Card {
                    ExerciseCard(
                        exercise = state.exercises[page],
                        onAction = onAction,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Exercise ${pagerState.currentPage + 1} / ${state.exercises.size}")
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "${exercise.sets.size} x ${exercise.exerciseDefinition.name} in ${exercise.repRange.first} - ${exercise.repRange.last}",
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))

        exercise.sets.forEachIndexed { index, set ->
            val weightState = rememberSyncedTextField(
                key = exercise,
                initialText = remember(exercise) { set.weight.kilograms },
            ) { weight ->
                onAction(WorkoutSessionAction.OnWeightUpdated(weight, exercise, index))
            }
            val repState = rememberSyncedTextField(
                key = exercise,
                initialText = remember(exercise) { set.reps.toString() },
            ) { reps ->
                onAction(WorkoutSessionAction.OnRepsUpdated(reps, exercise, index))
            }

            SetRow(
                exercise = exercise,
                index = index,
                set = set,
                weightState = weightState,
                repState = repState,
                onAction = onAction,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton({ onAction(WorkoutSessionAction.OnAddSet(exercise)) }) {
                Text("Add set")
            }

            if (exercise.canRemoveSet) {
                TextButton({ onAction(WorkoutSessionAction.RemoveLastSet(exercise)) }) {
                    Text("Remove last set")
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    exercise: WorkoutExercise,
    index: Int,
    set: ExerciseSet,
    weightState: TextFieldState,
    repState: TextFieldState,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .defaultMinSize(minHeight = 48.dp)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (set.setState) {
                SetState.Completed -> Text("${index + 1}. ${set.weight.kilograms} kg x ${set.reps}")
                SetState.InProgress -> {
                    val labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${index + 1}.",
                            color = labelColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        NumberInputField(weightState)
                        Text(
                            " kg x ",
                            color = labelColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        NumberInputField(repState)
                    }
                }

                SetState.NotStated -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${index + 1}.")
                    NumberInputField(weightState)
                    Text(" kg x ")
                    NumberInputField(repState)
                }
            }
            Spacer(Modifier.weight(1f))
            when (set.setState) {
                SetState.InProgress -> Checkbox(
                    false,
                    { onAction(WorkoutSessionAction.OnCompleteSet(exercise, index)) },
                    Modifier.clearAndSetSemantics { }
                )

                SetState.Completed -> Checkbox(
                    true,
                    null,
                    Modifier.padding(horizontal = 10.dp)
                )

                SetState.NotStated -> Unit
            }
        }
    }

    if (set.setState == SetState.InProgress) {
        Surface(
            onClick = { onAction(WorkoutSessionAction.OnCompleteSet(exercise, index)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "Set ${index + 1} in progress, ${set.weight.kilograms} kg by ${set.reps}. Double tap to mark complete."
                },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun rememberSyncedTextField(
    key: Any?,
    initialText: String,
    onTextChanged: (String) -> Unit,
): TextFieldState {
    val state = rememberSaveable(key, saver = TextFieldState.Saver) {
        TextFieldState(initialText)
    }
    LaunchedEffect(state, key) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collectLatest(onTextChanged)
    }
    return state
}

@Composable
private fun NumberInputField(state: TextFieldState) {
    TextField(
        state,
        Modifier.width(56.dp),
        textStyle = MaterialTheme.typography.labelLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Preview
@Composable
private fun SessionScreenContentPage1CompletedExercisePreview() {
    val session = fullBodyA.toWorkoutSession()
    SessionScreenContent(
        session.completeSet(session.exercises.first(), 0),
        {}
    )
}

@Preview
@Composable
private fun SessionScreenContentInitialPreview() {
    SessionScreenContent(fullBodyA.toWorkoutSession(), {})
}
