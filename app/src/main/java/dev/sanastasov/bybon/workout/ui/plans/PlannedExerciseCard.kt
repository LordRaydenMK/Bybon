package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.ui.ExerciseCardSetColWidth
import dev.sanastasov.bybon.workout.ui.ExerciseOverflow
import dev.sanastasov.bybon.workout.ui.ExerciseOverflowMenu
import dev.sanastasov.bybon.workout.ui.RestOrSpacer
import dev.sanastasov.bybon.workout.ui.SetNumberBadge

@Composable
fun PlannedExerciseCard(
    exercise: PlanedExercise,
    overflow: ExerciseOverflow?,
    onAddSet: () -> Unit,
    onRemoveLastSet: () -> Unit,
    onAddWarmupSet: () -> Unit,
    onRemoveLastWarmupSet: () -> Unit,
    onRemoveExercise: () -> Unit,
    modifier: Modifier = Modifier,
    canRemoveExercise: Boolean = true,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlannedExerciseHeader(exercise, overflow, onRemoveExercise)
        PlannedSetColumnsHeader()
        exercise.toPlannedSets().forEachIndexed { index, plannedSet ->
            key(index) {
                PlannedSetRow(exercise.exercise.name, plannedSet)
            }
        }
        PlannedExerciseActions(
            exercise,
            onAddSet,
            onRemoveLastSet,
            onAddWarmupSet,
            onRemoveLastWarmupSet,
            onRemoveExercise,
            canRemoveExercise,
        )
    }
}

@Composable
private fun PlannedExerciseHeader(
    exercise: PlanedExercise,
    overflow: ExerciseOverflow?,
    onRemoveExercise: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                exercise.exercise.name,
                fontWeight = FontWeight.Bold,
            )
            Text(
                exercise.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        overflow?.let {
            ExerciseOverflowMenu(exercise.exercise.name, it) { dismiss ->
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

@Composable
private fun PlannedSetColumnsHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell("set", ExerciseCardSetColWidth)
        Text(
            "reps",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        RestOrSpacer(null)
    }
}

@Composable
private fun HeaderCell(label: String, width: Dp) {
    Text(
        label,
        Modifier.width(width),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun PlannedSetRow(exerciseName: String, plannedSet: PlannedSetUi) {
    Box(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = plannedSet.contentDescription(exerciseName)
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.width(ExerciseCardSetColWidth),
                contentAlignment = Alignment.Center,
            ) {
                SetNumberBadge(
                    isWarmup = plannedSet.isWarmup,
                    workSetNumber = plannedSet.workSetNumber,
                    onClick = null,
                )
            }
            Text(
                plannedSet.repsLabel,
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
            )
            RestOrSpacer(plannedSet.rest)
        }
    }
}

@Composable
private fun PlannedExerciseActions(
    exercise: PlanedExercise,
    onAddSet: () -> Unit,
    onRemoveLastSet: () -> Unit,
    onAddWarmupSet: () -> Unit,
    onRemoveLastWarmupSet: () -> Unit,
    onRemoveExercise: () -> Unit,
    canRemoveExercise: Boolean,
) {
    val exerciseName = exercise.exercise.name
    Column(Modifier.fillMaxWidth()) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlanActionButton("Add Set", "Add set to $exerciseName", onAddSet)
            if (exercise.sets == 1) {
                if (canRemoveExercise) {
                    PlanActionButton(
                        "Remove Exercise",
                        "Remove $exerciseName from plan",
                        onRemoveExercise,
                    )
                }
            } else {
                PlanActionButton(
                    "Remove Last Set",
                    "Remove last set from $exerciseName",
                    onRemoveLastSet,
                )
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlanActionButton("Add Warmup", "Add warmup set to $exerciseName", onAddWarmupSet)
            if (exercise.warmupSets > 0) {
                PlanActionButton(
                    "Remove Warmup",
                    "Remove last warmup set from $exerciseName",
                    onRemoveLastWarmupSet,
                )
            }
        }
    }
}

@Composable
private fun PlanActionButton(label: String, description: String, onClick: () -> Unit) {
    TextButton(
        onClick,
        Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Text(label)
    }
}

@Preview
@Composable
private fun PlannedExerciseCardWithWarmupsPreview() {
    Surface {
        PlannedExerciseCard(
            fullBodyA.sets.first(),
            ExerciseOverflow(false, true, {}, {}),
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@Preview
@Composable
private fun PlannedExerciseCardWithoutWarmupsPreview() {
    Surface {
        PlannedExerciseCard(
            fullBodyA.sets.first { it.exercise.id == "seated-leg-curl" },
            ExerciseOverflow(true, true, {}, {}),
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@Preview
@Composable
private fun PlannedExerciseCardSingleSetPreview() {
    Surface {
        PlannedExerciseCard(
            fullBodyA.sets.first { it.exercise.id == "seated-leg-curl" }.copy(sets = 1),
            ExerciseOverflow(true, false, {}, {}),
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@Preview
@Composable
private fun PlannedExerciseCardLastExercisePreview() {
    Surface {
        PlannedExerciseCard(fullBodyA.sets.first(), null, {}, {}, {}, {}, {})
    }
}
