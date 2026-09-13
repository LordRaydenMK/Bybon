package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.ui.components.NumberInputField
import dev.sanastasov.bybon.ui.components.rememberSyncedTextField
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
import dev.sanastasov.bybon.workout.ui.SetNumberBadge

@Composable
fun WorkoutModule.WorkoutSessionScreen(planId: WorkoutPlanId) {
    val viewModel = retain {
        WorkoutSessionViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState?.let {
        SessionScreenContent(
            it,
            viewModel::onAction,
        )
    }
}

@Composable
private fun SessionScreenContent(state: WorkoutSession, onAction: (WorkoutSessionAction) -> Unit,) {
    Scaffold(
        topBar = { BybonTopAppBar(state.planName, {}) },
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
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
private fun ExerciseCard(exercise: WorkoutExercise, onAction: (WorkoutSessionAction) -> Unit,) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        val title =
            "${exercise.sets.size} x ${exercise.exerciseDefinition.name} " +
                "in ${exercise.repRange.first} - ${exercise.repRange.last}"
        Text(
            title,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))

        exercise.warmupSets?.let { warmupSets ->
            warmupSets.forEachIndexed { index, set ->
                val weightState = rememberSyncedTextField(
                    key = exercise to "w$index",
                    initialText = remember(exercise, index) { set.weight.kilograms },
                ) { weight ->
                    onAction(
                        WorkoutSessionAction.OnWeightUpdated(
                            weight,
                            exercise,
                            index,
                            isWarmup = true,
                        ),
                    )
                }
                val repState = rememberSyncedTextField(
                    key = exercise to "wr$index",
                    initialText = remember(exercise, index) { set.reps.toString() },
                ) { reps ->
                    onAction(
                        WorkoutSessionAction.OnRepsUpdated(
                            reps,
                            exercise,
                            index,
                            isWarmup = true,
                        ),
                    )
                }

                SetRow(
                    exercise = exercise,
                    index = index,
                    isWarmup = true,
                    workSetNumber = null,
                    set = set,
                    weightState = weightState,
                    repState = repState,
                    onBadgeClick = if (index == warmupSets.lastIndex) {
                        { onAction(WorkoutSessionAction.OnConvertToWorkSet(exercise)) }
                    } else {
                        null
                    },
                    onAction = onAction,
                )
            }
        }

        exercise.sets.forEachIndexed { index, set ->
            val weightState = rememberSyncedTextField(
                key = exercise to "s$index",
                initialText = remember(exercise, index) { set.weight.kilograms },
            ) { weight ->
                onAction(WorkoutSessionAction.OnWeightUpdated(weight, exercise, index))
            }
            val repState = rememberSyncedTextField(
                key = exercise to "sr$index",
                initialText = remember(exercise, index) { set.reps.toString() },
            ) { reps ->
                onAction(WorkoutSessionAction.OnRepsUpdated(reps, exercise, index))
            }

            SetRow(
                exercise = exercise,
                index = index,
                isWarmup = false,
                workSetNumber = index + 1,
                set = set,
                weightState = weightState,
                repState = repState,
                onBadgeClick = if (index == 0) {
                    { onAction(WorkoutSessionAction.OnConvertToWarmup(exercise)) }
                } else {
                    null
                },
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

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun SetRow(
    exercise: WorkoutExercise,
    index: Int,
    isWarmup: Boolean,
    workSetNumber: Int?,
    set: ExerciseSet,
    weightState: TextFieldState,
    repState: TextFieldState,
    onBadgeClick: (() -> Unit)?,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    val oneRmLabel = set.oneRm?.let { "@ ${it.kilograms} kg 1RM" }
    val previousLabel = set.previous?.let { previous ->
        buildString {
            append("${previous.weight.kilograms} kg x ${previous.reps}")
            previous.oneRm?.let { append(" @ ${it.kilograms} kg 1RM") }
        }
    }
    val setLabel = if (isWarmup) "Warmup set" else "Set $workSetNumber"
    val setDescription = buildString {
        append(setLabel)
        if (set.setState == SetState.InProgress) {
            append(" in progress")
        }
        append(", ${set.weight.kilograms} kg by ${set.reps}")
        if (oneRmLabel != null) {
            append(", $oneRmLabel")
        }
        if (previousLabel != null) {
            append(". Previous: $previousLabel")
        }
        if (set.setState == SetState.InProgress) {
            append(". Double tap to mark complete.")
        }
    }
    val content: @Composable () -> Unit = {
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
                when (set.setState) {
                    SetState.Completed -> {
                        val summary = buildString {
                            append("${set.weight.kilograms} kg x ${set.reps}")
                            if (oneRmLabel != null) {
                                append(" $oneRmLabel")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SetNumberBadge(
                                isWarmup = isWarmup,
                                workSetNumber = workSetNumber,
                                onClick = onBadgeClick,
                            )
                            Text(summary)
                        }
                    }

                    SetState.InProgress -> {
                        val labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SetNumberBadge(
                                isWarmup = isWarmup,
                                workSetNumber = workSetNumber,
                                onClick = onBadgeClick,
                                color = labelColor,
                                fontWeight = FontWeight.Bold,
                            )
                            NumberInputField(weightState)
                            Text(
                                " kg x ",
                                color = labelColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            NumberInputField(repState)
                            if (oneRmLabel != null) {
                                Text(
                                    oneRmLabel,
                                    color = labelColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }

                    SetState.NotStated -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SetNumberBadge(
                            isWarmup = isWarmup,
                            workSetNumber = workSetNumber,
                            onClick = onBadgeClick,
                        )
                        NumberInputField(weightState)
                        Text(" kg x ")
                        NumberInputField(repState)
                        if (oneRmLabel != null) {
                            Text(oneRmLabel)
                        }
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
            when (set.setState) {
                SetState.InProgress -> Checkbox(
                    false,
                    {
                        onAction(
                            WorkoutSessionAction.OnCompleteSet(exercise, index, isWarmup),
                        )
                    },
                    Modifier.clearAndSetSemantics { },
                )

                SetState.Completed -> Checkbox(
                    true,
                    null,
                    Modifier.padding(horizontal = 10.dp),
                )

                SetState.NotStated -> Unit
            }
        }
    }

    if (set.setState == SetState.InProgress) {
        Surface(
            onClick = {
                onAction(WorkoutSessionAction.OnCompleteSet(exercise, index, isWarmup))
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = setDescription
                },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            content()
        }
    } else if (set.setState == SetState.NotStated) {
        Box(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = setDescription
                },
        ) {
            content()
        }
    } else {
        content()
    }
}

@Preview
@Composable
private fun SessionScreenContentPage1CompletedExercisePreview() {
    val session = fullBodyA.toWorkoutSession()
    SessionScreenContent(
        session.completeSet(session.exercises.first(), 0, isWarmup = true),
        {},
    )
}

@Preview
@Composable
private fun SessionScreenContentInitialPreview() {
    SessionScreenContent(fullBodyA.toWorkoutSession(), {})
}
