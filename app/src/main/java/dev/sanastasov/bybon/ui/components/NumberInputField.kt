package dev.sanastasov.bybon.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

val NumberInputWeightMinWidth = 56.dp
val NumberInputRepsMinWidth = 36.dp

/**
 * [key] should be an identity that does not change when the parsed value changes.
 * `Weight` stores hundredths, so typing a digit updates weight and 1RM and would recreate
 * a field keyed on the whole `WorkoutExercise`.
 */
@Composable
fun NumberInputField(
    key: Any?,
    initialText: String,
    minWidth: Dp,
    onTextChanged: (String) -> Unit,
) {
    val state = rememberSaveable(key, saver = TextFieldState.Saver) {
        TextFieldState(initialText)
    }
    var focused by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key, initialText, focused) {
        if (!focused && state.text.toString() != initialText) {
            state.setTextAndPlaceCursorAtEnd(initialText)
        }
    }
    LaunchedEffect(state, key) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collectLatest(onTextChanged)
    }
    val textStyle = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
    val fieldWidth = rememberNumberInputWidth(state.text.toString(), minWidth, textStyle)
    TextField(
        state,
        Modifier
            .width(fieldWidth)
            .height(NumberInputHeight)
            .defaultMinSize(minWidth = minWidth, minHeight = NumberInputHeight)
            .onFocusChanged { focused = it.isFocused },
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    )
}

private val NumberInputHeight = 40.dp

@Composable
private fun rememberNumberInputWidth(text: String, minWidth: Dp, textStyle: TextStyle): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val sample = text.ifEmpty { "0" }
    val measured = with(density) {
        textMeasurer.measure(text = sample, style = textStyle).size.width.toDp() + 16.dp
    }
    return max(minWidth, measured)
}
