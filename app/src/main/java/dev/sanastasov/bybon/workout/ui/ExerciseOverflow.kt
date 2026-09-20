package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

data class ExerciseOverflow(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
)

@Composable
fun ExerciseOverflowMenu(
    exerciseName: String,
    overflow: ExerciseOverflow? = null,
    extraItems: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options for $exerciseName",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            overflow?.let { current ->
                DropdownMenuItem(
                    text = { Text("Move up") },
                    onClick = {
                        expanded = false
                        current.onMoveUp()
                    },
                    enabled = current.canMoveUp,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "Move $exerciseName up"
                    },
                )
                DropdownMenuItem(
                    text = { Text("Move down") },
                    onClick = {
                        expanded = false
                        current.onMoveDown()
                    },
                    enabled = current.canMoveDown,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "Move $exerciseName down"
                    },
                )
            }
            extraItems { expanded = false }
        }
    }
}
