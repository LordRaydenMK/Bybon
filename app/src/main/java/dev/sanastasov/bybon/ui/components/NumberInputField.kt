package dev.sanastasov.bybon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val NumberInputWeightMinWidth = 56.dp
val NumberInputRepsMinWidth = 36.dp

/**
 * [key] should be an identity that does not change when the parsed value changes.
 * `Weight` stores hundredths, so typing a digit updates weight and 1RM and would recreate
 * a field keyed on the whole `WorkoutExercise`.
 *
 * Material `TextField` is too heavy for the overview list: each exercise card hosts many
 * weight/reps inputs, and decoration layout plus per-field text measuring janks scrolling
 * in debug builds. [BasicTextField] keeps the same editing behavior with a shallow layout.
 */
@Composable
fun NumberInputField(
    key: Any?,
    initialText: String,
    minWidth: Dp,
    onTextChanged: (String) -> Unit,
) {
    var text by rememberSaveable(key) { mutableStateOf(initialText) }
    var focused by remember(key) { mutableStateOf(false) }
    val onTextChangedUpdated by rememberUpdatedState(onTextChanged)
    val value = if (focused) text else initialText
    val textStyle = MaterialTheme.typography.labelMedium.copy(
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
    val cursorColor = MaterialTheme.colorScheme.primary
    val indicatorColor = if (focused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNFOCUSED_INDICATOR_ALPHA)
    }
    val indicatorWidth = if (focused) FocusedIndicatorWidth else UnfocusedIndicatorWidth
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            text = newValue
            onTextChangedUpdated(newValue)
        },
        modifier = Modifier
            .width(minWidth)
            .height(NumberInputHeight)
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !focused) {
                    text = initialText
                }
                focused = focusState.isFocused
            },
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        cursorBrush = SolidColor(cursorColor),
        decorationBox = { innerTextField ->
            Box(
                Modifier
                    .fillMaxSize()
                    .drawIndicatorLine(indicatorColor, indicatorWidth),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

private val NumberInputHeight = 40.dp
private val UnfocusedIndicatorWidth = 1.dp
private val FocusedIndicatorWidth = 2.dp
private const val UNFOCUSED_INDICATOR_ALPHA = 0.42f

private fun Modifier.drawIndicatorLine(color: Color, width: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    val y = size.height - stroke / 2f
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = stroke,
    )
}
