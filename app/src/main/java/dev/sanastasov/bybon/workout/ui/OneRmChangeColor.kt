package dev.sanastasov.bybon.workout.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.sanastasov.bybon.workout.domain.OneRmChange

private val OneRmImproved = Color(0xFF1B7A3D)

@Composable
fun OneRmChange?.oneRmChangeColor(unchanged: Color = Color.Unspecified): Color = when (this) {
    is OneRmChange.Improved -> OneRmImproved
    is OneRmChange.Decreased -> MaterialTheme.colorScheme.error
    OneRmChange.Unchanged, null -> unchanged
}
