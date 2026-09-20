package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.ui.components.NumberInputRepsMinWidth
import dev.sanastasov.bybon.ui.components.NumberInputWeightMinWidth
import dev.sanastasov.bybon.workout.domain.WorkoutExercise

internal val ExerciseCardSetColWidth = 28.dp
internal val ExerciseCardPreviousColWidth = 64.dp
internal val ExerciseCardWeightColWidth = NumberInputWeightMinWidth
internal val ExerciseCardXColWidth = 16.dp
internal val ExerciseCardRepsColWidth = NumberInputRepsMinWidth
internal val ExerciseCardOneRmColWidth = 48.dp
internal val ExerciseCardTrailingColWidth = 56.dp

@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    mode: ExerciseCardMode,
    onEvent: (ExerciseCardEvent) -> Unit,
    modifier: Modifier = Modifier,
    overflow: ExerciseOverflow? = null,
    onRemoveExercise: (() -> Unit)? = null,
) {
    val scrollable = if (mode == ExerciseCardMode.Session) {
        modifier.verticalScroll(rememberScrollState())
    } else {
        modifier
    }
    Column(
        scrollable
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ExerciseCardHeader(exercise, overflow, onRemoveExercise, onEvent)
        SetColumnsHeader(
            canResetAll = exercise.hasPreviousPerformance,
            onResetAllSets = { onEvent(ExerciseCardEvent.OnResetAllSets) },
        )
        val warmupSets = exercise.numberedWarmupSets
        warmupSets?.forEach { numbered ->
            ExerciseSetBlock(
                exercise = exercise,
                numbered = numbered,
                mode = mode,
                onEvent = onEvent,
                onBadgeClick = if (numbered.index == warmupSets.lastIndex) {
                    { onEvent(ExerciseCardEvent.OnConvertToWorkSet) }
                } else {
                    null
                },
            )
        }
        exercise.numberedWorkSets.forEach { numbered ->
            ExerciseSetBlock(
                exercise = exercise,
                numbered = numbered,
                mode = mode,
                onEvent = onEvent,
                onBadgeClick = if (numbered.index == 0 && exercise.sets.size > 1) {
                    { onEvent(ExerciseCardEvent.OnConvertToWarmup) }
                } else {
                    null
                },
                rest = exercise.restAfterWorkSet,
            )
        }
        ExerciseSetActions(exercise, mode, onEvent)
    }
}

@Composable
private fun ExerciseCardHeader(
    exercise: WorkoutExercise,
    overflow: ExerciseOverflow?,
    onRemoveExercise: (() -> Unit)?,
    onEvent: (ExerciseCardEvent) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${exercise.sets.size} x ${exercise.exerciseDefinition.name} " +
                "in ${exercise.repRange.first} - ${exercise.repRange.last}",
            Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                { onEvent(ExerciseCardEvent.OnDecrease) },
                Modifier.semantics {
                    contentDescription =
                        "Decrease weight and reps for ${exercise.exerciseDefinition.name}"
                },
            ) {
                Text(
                    "−",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            IconButton(
                { onEvent(ExerciseCardEvent.OnIncrease) },
                Modifier.semantics {
                    contentDescription =
                        "Increase weight and reps for ${exercise.exerciseDefinition.name}"
                },
            ) {
                Text(
                    "+",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            overflow?.let { current ->
                ExerciseOverflowMenu(exercise.exerciseDefinition.name, current) { dismiss ->
                    if (onRemoveExercise != null) {
                        DropdownMenuItem(
                            text = { Text("Remove Exercise") },
                            onClick = {
                                dismiss()
                                onRemoveExercise()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetColumnsHeader(canResetAll: Boolean, onResetAllSets: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell("", ExerciseCardSetColWidth)
        HeaderCell(
            "previous",
            ExerciseCardPreviousColWidth,
            modifier = if (canResetAll) {
                Modifier
                    .clickable(onClick = onResetAllSets)
                    .clearAndSetSemantics {
                        contentDescription = "Reset all sets to previous session"
                    }
            } else {
                Modifier
            },
        )
        HeaderCell("kg", ExerciseCardWeightColWidth)
        Spacer(Modifier.width(ExerciseCardXColWidth))
        HeaderCell("reps", ExerciseCardRepsColWidth)
        HeaderCell("1RM", ExerciseCardOneRmColWidth)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(ExerciseCardTrailingColWidth))
    }
}

@Composable
private fun HeaderCell(label: String, width: Dp? = null, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier.then(if (width != null) Modifier.width(width) else Modifier),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ExerciseSetActions(
    exercise: WorkoutExercise,
    mode: ExerciseCardMode,
    onEvent: (ExerciseCardEvent) -> Unit,
) {
    val canRemove = when (mode) {
        ExerciseCardMode.Overview -> exercise.sets.size > 1
        ExerciseCardMode.Session -> exercise.canRemoveSet
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton({ onEvent(ExerciseCardEvent.OnAddSet) }) {
            Text("Add set")
        }
        if (canRemove) {
            TextButton({ onEvent(ExerciseCardEvent.OnRemoveLastSet) }) {
                Text("Remove last set")
            }
        }
    }
}
