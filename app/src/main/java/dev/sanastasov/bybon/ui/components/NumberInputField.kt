package dev.sanastasov.bybon.ui.components

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * [key] should be an identity that does not change when the parsed value changes.
 * `Weight` stores hundredths, so typing a digit updates weight and 1RM and would recreate
 * a field keyed on the whole `WorkoutExercise`.
 */
@Composable
fun NumberInputField(key: Any?, initialText: String, onTextChanged: (String) -> Unit) {
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
    TextField(
        state,
        Modifier
            .width(72.dp)
            .onFocusChanged { focused = it.isFocused },
        textStyle = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
        ),
    )
}
