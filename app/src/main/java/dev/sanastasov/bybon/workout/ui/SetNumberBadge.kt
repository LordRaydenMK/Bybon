package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SetNumberBadge(
    isWarmup: Boolean,
    workSetNumber: Int?,
    onClick: (() -> Unit)?,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val label = if (isWarmup) "w" else "$workSetNumber."
    val description = when {
        isWarmup && onClick != null -> "Warmup set, double tap to convert to work set"
        isWarmup -> "Warmup set"
        onClick != null -> "Set $workSetNumber, double tap to convert to warmup set"
        else -> "Set $workSetNumber"
    }
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        color = color,
        fontWeight = fontWeight ?: FontWeight.Normal,
    )
    if (isWarmup) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .border(1.dp, color.takeOr(MaterialTheme.colorScheme.onSurface), CircleShape)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Text("w", style = textStyle, fontWeight = FontWeight.Bold)
        }
    } else {
        Text(
            label,
            Modifier
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .semantics { contentDescription = description },
            color = color,
            fontWeight = fontWeight,
        )
    }
}

private fun Color.takeOr(fallback: Color): Color = if (this == Color.Unspecified) fallback else this
