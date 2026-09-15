package dev.sanastasov.bybon.workout.ui.session

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.sanastasov.bybon.ui.components.NumberInputRepsMinWidth
import dev.sanastasov.bybon.ui.components.NumberInputWeightMinWidth
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.ui.SetNumberBadge
import dev.sanastasov.bybon.workout.ui.SetPreviousAndRestRow
import dev.sanastasov.bybon.workout.ui.completedContentDescription
import dev.sanastasov.bybon.workout.ui.oneRmLabel
import dev.sanastasov.bybon.workout.ui.previousLabel
import dev.sanastasov.bybon.workout.ui.sessionContentDescription
import dev.sanastasov.bybon.workout.ui.weightRepsLabel
import kotlin.time.Duration

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
private fun SessionScreenContent(state: WorkoutSession, onAction: (WorkoutSessionAction) -> Unit) {
    Scaffold(
        topBar = { BybonTopAppBar(state.planName, {}) },
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
            ) { page ->
                Card(Modifier.fillMaxSize()) {
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
private fun ExerciseCard(exercise: WorkoutExercise, onAction: (WorkoutSessionAction) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val title =
            "${exercise.sets.size} x ${exercise.exerciseDefinition.name} " +
                "in ${exercise.repRange.first} - ${exercise.repRange.last}"
        Text(
            title,
            fontWeight = FontWeight.Bold,
        )
        SessionWarmupSets(exercise, onAction)
        SessionWorkSets(exercise, onAction)
        SessionSetActions(exercise, onAction)
    }
}

@Composable
private fun SessionWarmupSets(
    exercise: WorkoutExercise,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    val warmupSets = exercise.numberedWarmupSets ?: return
    warmupSets.forEach { numbered ->
        SetRow(
            exercise = exercise,
            numbered = numbered,
            onBadgeClick = if (numbered.index == warmupSets.lastIndex) {
                { onAction(WorkoutSessionAction.OnConvertToWorkSet(exercise)) }
            } else {
                null
            },
            onAction = onAction,
        )
    }
}

@Composable
private fun SessionWorkSets(exercise: WorkoutExercise, onAction: (WorkoutSessionAction) -> Unit) {
    exercise.numberedWorkSets.forEach { numbered ->
        SetRow(
            exercise = exercise,
            numbered = numbered,
            onBadgeClick = if (numbered.index == 0) {
                { onAction(WorkoutSessionAction.OnConvertToWarmup(exercise)) }
            } else {
                null
            },
            onAction = onAction,
            rest = exercise.restAfterWorkSet,
        )
    }
}

@Composable
private fun SessionSetActions(exercise: WorkoutExercise, onAction: (WorkoutSessionAction) -> Unit) {
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

@Composable
private fun SessionWeightField(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    val slot = if (numbered.isWarmup) "w" else "s"
    NumberInputField(
        key = "${exercise.id}-$slot${numbered.index}-weight",
        initialText = numbered.set.weight.kilograms,
        minWidth = NumberInputWeightMinWidth,
    ) { weight ->
        onAction(
            WorkoutSessionAction.OnWeightUpdated(
                weight,
                exercise,
                numbered.index,
                numbered.isWarmup,
            ),
        )
    }
}

@Composable
private fun SessionRepsField(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    val slot = if (numbered.isWarmup) "w" else "s"
    NumberInputField(
        key = "${exercise.id}-$slot${numbered.index}-reps",
        initialText = numbered.set.reps.toString(),
        minWidth = NumberInputRepsMinWidth,
    ) { reps ->
        onAction(
            WorkoutSessionAction.OnRepsUpdated(
                reps,
                exercise,
                numbered.index,
                numbered.isWarmup,
            ),
        )
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun SetRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onBadgeClick: (() -> Unit)?,
    onAction: (WorkoutSessionAction) -> Unit,
    rest: Duration? = null,
) {
    val set = numbered.set
    val oneRmLabel = numbered.oneRmLabel
    val previousLabel = numbered.previousLabel
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .defaultMinSize(minHeight = 40.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                when (set.setState) {
                    SetState.Completed -> {
                        val summary = buildString {
                            append(numbered.weightRepsLabel)
                            if (oneRmLabel != null) {
                                append(" $oneRmLabel")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SetNumberBadge(
                                isWarmup = numbered.isWarmup,
                                workSetNumber = numbered.workSetNumber,
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
                                isWarmup = numbered.isWarmup,
                                workSetNumber = numbered.workSetNumber,
                                onClick = onBadgeClick,
                                color = labelColor,
                                fontWeight = FontWeight.Bold,
                            )
                            SessionWeightField(exercise, numbered, onAction)
                            Text(
                                " kg x ",
                                color = labelColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            SessionRepsField(exercise, numbered, onAction)
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
                            isWarmup = numbered.isWarmup,
                            workSetNumber = numbered.workSetNumber,
                            onClick = onBadgeClick,
                        )
                        SessionWeightField(exercise, numbered, onAction)
                        Text(" kg x ")
                        SessionRepsField(exercise, numbered, onAction)
                        if (oneRmLabel != null) {
                            Text(oneRmLabel)
                        }
                    }
                }
            }
            when (set.setState) {
                SetState.InProgress -> Checkbox(
                    false,
                    {
                        onAction(
                            WorkoutSessionAction.OnCompleteSet(
                                exercise,
                                numbered.index,
                                numbered.isWarmup,
                            ),
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

    Column(Modifier.fillMaxWidth()) {
        if (set.setState == SetState.InProgress) {
            Surface(
                onClick = {
                    onAction(
                        WorkoutSessionAction.OnCompleteSet(
                            exercise,
                            numbered.index,
                            numbered.isWarmup,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = numbered.sessionContentDescription
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
                        contentDescription = numbered.sessionContentDescription
                    },
            ) {
                content()
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = numbered.completedContentDescription
                    },
            ) {
                content()
            }
        }
        SetPreviousAndRestRow(
            previousLabel,
            rest,
            Modifier.padding(horizontal = 8.dp),
        )
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
