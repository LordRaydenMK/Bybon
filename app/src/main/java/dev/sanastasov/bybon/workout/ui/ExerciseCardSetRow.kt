package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sanastasov.bybon.ui.components.NumberInputField
import dev.sanastasov.bybon.ui.components.NumberInputRepsMinWidth
import dev.sanastasov.bybon.ui.components.NumberInputWeightMinWidth
import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import kotlin.time.Duration

@Composable
internal fun SetValuesRow(
    numbered: NumberedSet,
    onBadgeClick: (() -> Unit)?,
    onResetSet: () -> Unit,
    weight: @Composable () -> Unit,
    reps: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    color: Color = Color.Unspecified,
    badgeColor: Color = Color.Unspecified,
    badgeFontWeight: FontWeight? = null,
    valueFontWeight: FontWeight? = null,
) {
    val oneRm = numbered.set.oneRm
        .takeUnless { numbered.isWarmup }
        ?.let { formatOneRmKg(it.kilogramsValue) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetBadgeColumn(numbered, onBadgeClick, badgeColor, badgeFontWeight)
        PreviousColumn(numbered, onResetSet)
        Box(Modifier.width(ExerciseCardWeightColWidth), contentAlignment = Alignment.Center) {
            weight()
        }
        Text(
            "x",
            Modifier.width(ExerciseCardXColWidth),
            color = color,
            fontWeight = valueFontWeight,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.width(ExerciseCardRepsColWidth), contentAlignment = Alignment.Center) {
            reps()
        }
        Text(
            oneRm.orEmpty(),
            Modifier.width(ExerciseCardOneRmColWidth),
            color = color,
            fontWeight = valueFontWeight,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.width(ExerciseCardTrailingColWidth),
            contentAlignment = Alignment.Center,
        ) {
            trailing()
        }
    }
}

@Composable
private fun SetBadgeColumn(
    numbered: NumberedSet,
    onBadgeClick: (() -> Unit)?,
    badgeColor: Color,
    badgeFontWeight: FontWeight?,
) {
    Box(Modifier.width(ExerciseCardSetColWidth), contentAlignment = Alignment.Center) {
        SetNumberBadge(
            isWarmup = numbered.isWarmup,
            workSetNumber = numbered.workSetNumber,
            onClick = onBadgeClick,
            color = badgeColor,
            fontWeight = badgeFontWeight,
        )
    }
}

@Composable
private fun PreviousColumn(numbered: NumberedSet, onResetSet: () -> Unit) {
    val previous = numbered.set.previous
    Text(
        previous?.let { "${it.weight.kilograms} x ${it.reps}" }.orEmpty(),
        Modifier
            .width(ExerciseCardPreviousColWidth)
            .then(
                if (previous != null) {
                    Modifier
                        .clickable(onClick = onResetSet)
                        .clearAndSetSemantics {
                            contentDescription = "Reset set to previous session"
                        }
                } else {
                    Modifier
                },
            ),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun RestOrSpacer(rest: Duration?) {
    if (rest != null) {
        RestTimerRow(rest, Modifier.width(ExerciseCardTrailingColWidth))
    } else {
        Spacer(Modifier.width(ExerciseCardTrailingColWidth))
    }
}

@Composable
internal fun EditableWeight(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
) {
    WeightField(exercise, numbered) { weight ->
        onEvent(
            ExerciseCardEvent.OnWeightUpdated(weight, numbered.index, numbered.isWarmup),
        )
    }
}

@Composable
internal fun EditableReps(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onEvent: (ExerciseCardEvent) -> Unit,
) {
    RepsField(exercise, numbered) { reps ->
        onEvent(
            ExerciseCardEvent.OnRepsUpdated(reps, numbered.index, numbered.isWarmup),
        )
    }
}

@Composable
internal fun ColumnValue(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun WeightField(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onWeightChanged: (String) -> Unit,
) {
    val slot = if (numbered.isWarmup) "w" else "s"
    NumberInputField(
        key = "${exercise.id}-$slot${numbered.index}-weight",
        initialText = numbered.set.weight.kilograms,
        minWidth = NumberInputWeightMinWidth,
        onTextChanged = onWeightChanged,
    )
}

@Composable
private fun RepsField(
    exercise: WorkoutExercise,
    numbered: NumberedSet,
    onRepsChanged: (String) -> Unit,
) {
    val slot = if (numbered.isWarmup) "w" else "s"
    NumberInputField(
        key = "${exercise.id}-$slot${numbered.index}-reps",
        initialText = numbered.set.reps.toString(),
        minWidth = NumberInputRepsMinWidth,
        onTextChanged = onRepsChanged,
    )
}
