package dev.sanastasov.bybon.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun rememberSyncedTextField(
    key: Any?,
    initialText: String,
    onTextChanged: (String) -> Unit
): TextFieldState {
    val state = rememberSaveable(key, saver = TextFieldState.Saver) {
        TextFieldState(initialText)
    }
    LaunchedEffect(state, key) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collectLatest(onTextChanged)
    }
    return state
}

@Composable
fun NumberInputField(state: TextFieldState) {
    TextField(
        state,
        Modifier.width(72.dp),
        textStyle = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent
        )
    )
}
