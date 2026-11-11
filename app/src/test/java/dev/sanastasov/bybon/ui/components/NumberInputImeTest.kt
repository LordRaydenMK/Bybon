package dev.sanastasov.bybon.ui.components

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import org.junit.Test

class NumberInputImeTest {

    @Test
    fun `done dismisses keyboard and clears focus`() {
        val focusManager = RecordingFocusManager()
        val keyboard = RecordingKeyboardController()

        dismissNumberInputIme(focusManager, keyboard)

        assert(focusManager.cleared)
        assert(keyboard.hidden)
    }
}

private class RecordingFocusManager : FocusManager {
    var cleared = false
        private set

    override fun clearFocus(force: Boolean) {
        cleared = true
    }

    override fun moveFocus(focusDirection: FocusDirection): Boolean = false
}

private class RecordingKeyboardController : SoftwareKeyboardController {
    var hidden = false
        private set

    override fun show() = Unit

    override fun hide() {
        hidden = true
    }
}
