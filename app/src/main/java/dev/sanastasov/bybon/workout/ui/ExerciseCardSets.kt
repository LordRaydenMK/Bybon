package dev.sanastasov.bybon.workout.ui

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

@Composable
internal fun ExerciseSetBlock(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    mode: ExerciseCardMode,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
) {
    when (mode) {
        ExerciseCardMode.Overview -> OverviewSetValuesRow(
            exercise,
            numbered,
            onEvent,
            onBadgeClick,
        )

        ExerciseCardMode.Session -> SessionSetBlock(
            exercise,
            numbered,
            onEvent,
            onBadgeClick,
        )
    }
}

@Composable
private fun SessionSetBlock(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
) {
    if (numbered.set.setState == SetState.InProgress) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = numbered.sessionContentDescription
                },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            SessionSetValuesRow(exercise, numbered, onEvent, onBadgeClick)
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = if (numbered.set.setState is SetState.Completed) {
                        numbered.completedContentDescription
                    } else {
                        numbered.sessionContentDescription
                    }
                },
        ) {
            SessionSetValuesRow(exercise, numbered, onEvent, onBadgeClick)
        }
    }
}

@Composable
private fun OverviewSetValuesRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
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
            trailing = { RestOrSpacer(null) },
        )
    }
}

@Composable
private fun SessionSetValuesRow(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
) {
    when (numbered.set.setState) {
        is SetState.Completed -> CompletedSessionSetRow(numbered, onEvent, onBadgeClick)
        SetState.InProgress -> InProgressSessionSetRow(exercise, numbered, onEvent, onBadgeClick)
        SetState.NotStated -> PendingSessionSetRow(exercise, numbered, onEvent, onBadgeClick)
    }
}

@Composable
private fun CompletedSessionSetRow(
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
    onBadgeClick: (() -> Unit)?,
) {
    val set = numbered.set
    SetValuesRow(
        numbered = numbered,
        onBadgeClick = onBadgeClick,
        onResetSet = {
            onEvent(ExerciseCardEvent.OnResetSet(numbered.index, numbered.isWarmup))
        },
        weight = { ColumnValue(set.weight.toLoadLabel()) },
        reps = { ColumnValue(set.reps.toString()) },
        trailing = {
            Checkbox(
                true,
                null,
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
                    .semantics {
                        contentDescription = numbered.completeSetContentDescription
                    },
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
) {
    SetValuesRow(
        numbered = numbered,
        onBadgeClick = onBadgeClick,
        onResetSet = {
            onEvent(ExerciseCardEvent.OnResetSet(numbered.index, numbered.isWarmup))
        },
        weight = { EditableWeight(exercise, numbered, onEvent) },
        reps = { EditableReps(exercise, numbered, onEvent) },
        trailing = { RestOrSpacer(null) },
    )
}
