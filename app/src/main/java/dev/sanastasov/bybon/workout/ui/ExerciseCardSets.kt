package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import kotlin.time.Duration

@Composable
internal fun ExerciseSetBlock(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    mode: ExerciseCardMode,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
    rest: Duration? = null,
    canUncomplete: Boolean = false,
) {
    when (mode) {
        ExerciseCardMode.Overview -> OverviewSetValuesRow(
            exercise,
            numbered,
            onEvent,
            onBadgeClick,
            rest,
        )

        ExerciseCardMode.Session -> SessionSetBlock(
            exercise,
            numbered,
            onEvent,
            onBadgeClick,
            rest,
            canUncomplete,
        )
    }
}

@Composable
private fun SessionSetBlock(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
    rest: Duration?,
    canUncomplete: Boolean,
) {
    val description = numbered.sessionRowContentDescription(canUncomplete)
    when {
        numbered.set.setState == SetState.InProgress -> Surface(
            onClick = {
                onEvent(ExerciseCardEvent.OnCompleteSet(numbered.index, numbered.isWarmup))
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = description },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            SessionSetValuesRow(exercise, numbered, onEvent, onBadgeClick, rest, canUncomplete)
        }

        numbered.set.setState == SetState.Completed && canUncomplete -> Box(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent(ExerciseCardEvent.OnUncompleteSet(numbered.index, numbered.isWarmup))
                }
                .semantics(mergeDescendants = true) { contentDescription = description },
        ) {
            SessionSetValuesRow(exercise, numbered, onEvent, onBadgeClick, rest, canUncomplete)
        }

        else -> Box(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = description },
        ) {
            SessionSetValuesRow(exercise, numbered, onEvent, onBadgeClick, rest, canUncomplete)
        }
    }
}

private fun NumberedSet.sessionRowContentDescription(canUncomplete: Boolean): String = when {
    set.setState == SetState.Completed && canUncomplete -> uncompleteContentDescription
    set.setState == SetState.Completed -> completedContentDescription
    else -> sessionContentDescription
}

@Composable
private fun OverviewSetValuesRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
    rest: Duration?,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = numbered.overviewContentDescription
            },
    ) {
        SetValuesRow(
            numbered = numbered,
            onBadgeClick = onBadgeClick,
            onResetSet = {
                onEvent(ExerciseCardEvent.OnResetSet(numbered.index, numbered.isWarmup))
            },
            weight = { EditableWeight(exercise, numbered, onEvent) },
            reps = { EditableReps(exercise, numbered, onEvent) },
            trailing = { RestOrSpacer(rest) },
        )
    }
}

@Composable
private fun SessionSetValuesRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
    rest: Duration?,
    canUncomplete: Boolean,
) {
    when (numbered.set.setState) {
        SetState.Completed -> CompletedSessionSetRow(numbered, onEvent, onBadgeClick, canUncomplete)
        SetState.InProgress -> InProgressSessionSetRow(exercise, numbered, onEvent, onBadgeClick)
        SetState.NotStated -> PendingSessionSetRow(exercise, numbered, onEvent, onBadgeClick, rest)
    }
}

@Composable
private fun CompletedSessionSetRow(
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
    canUncomplete: Boolean,
) {
    val set = numbered.set
    SetValuesRow(
        numbered = numbered,
        onBadgeClick = onBadgeClick,
        onResetSet = {
            onEvent(ExerciseCardEvent.OnResetSet(numbered.index, numbered.isWarmup))
        },
        weight = { ColumnValue(set.weight.kilograms) },
        reps = { ColumnValue(set.reps.toString()) },
        trailing = {
            Checkbox(
                true,
                if (canUncomplete) {
                    {
                        onEvent(
                            ExerciseCardEvent.OnUncompleteSet(numbered.index, numbered.isWarmup),
                        )
                    }
                } else {
                    null
                },
                Modifier
                    .width(ExerciseCardTrailingColWidth)
                    .clearAndSetSemantics { },
            )
        },
    )
}

@Composable
private fun InProgressSessionSetRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
) {
    val labelColor = MaterialTheme.colorScheme.onTertiaryContainer
    SetValuesRow(
        numbered = numbered,
        onBadgeClick = onBadgeClick,
        onResetSet = {
            onEvent(ExerciseCardEvent.OnResetSet(numbered.index, numbered.isWarmup))
        },
        weight = { EditableWeight(exercise, numbered, onEvent) },
        reps = { EditableReps(exercise, numbered, onEvent) },
        color = labelColor,
        badgeColor = labelColor,
        badgeFontWeight = FontWeight.Bold,
        valueFontWeight = FontWeight.Bold,
        trailing = {
            Checkbox(
                false,
                {
                    onEvent(
                        ExerciseCardEvent.OnCompleteSet(numbered.index, numbered.isWarmup),
                    )
                },
                Modifier
                    .width(ExerciseCardTrailingColWidth)
                    .clearAndSetSemantics { },
            )
        },
    )
}

@Composable
private fun PendingSessionSetRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
    rest: Duration?,
) {
    SetValuesRow(
        numbered = numbered,
        onBadgeClick = onBadgeClick,
        onResetSet = {
            onEvent(ExerciseCardEvent.OnResetSet(numbered.index, numbered.isWarmup))
        },
        weight = { EditableWeight(exercise, numbered, onEvent) },
        reps = { EditableReps(exercise, numbered, onEvent) },
        trailing = { RestOrSpacer(rest) },
    )
}
