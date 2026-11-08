package dev.sanastasov.bybon.bodyweight.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import dev.sanastasov.bybon.domain.toDisplayDate

@Composable
fun WeightSyncPromptDialogs(prompt: WeightSyncPrompt, onAction: (WeightSyncPromptAction) -> Unit) {
    when (prompt) {
        WeightSyncPrompt.None -> Unit
        WeightSyncPrompt.Conflict -> WeightConflictDialog(onAction)
        is WeightSyncPrompt.Summary -> WeightImportDialog(prompt.summary, onAction)
    }
}

@Composable
private fun WeightConflictDialog(onAction: (WeightSyncPromptAction) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Weights differ") },
        text = { Text("The backup would change weights saved on this phone.") },
        confirmButton = {
            Column {
                TextButton({ onAction(WeightSyncPromptAction.UseTheBackup) }) {
                    Text("Use the backup")
                }
                TextButton({ onAction(WeightSyncPromptAction.ReplaceTheBackup) }) {
                    Text("Replace the backup")
                }
                TextButton({ onAction(WeightSyncPromptAction.KeepThisPhone) }) {
                    Text("Keep this phone")
                }
            }
        },
    )
}

@Composable
private fun WeightImportDialog(
    summary: WeightImportSummary,
    onAction: (WeightSyncPromptAction) -> Unit,
) {
    val noun = if (summary.count == 1) "weight" else "weights"
    AlertDialog(
        onDismissRequest = { onAction(WeightSyncPromptAction.DismissSummary) },
        title = { Text("Weights imported") },
        text = {
            Text(
                "Imported ${summary.count} $noun, from " +
                    "${summary.earliest.toDisplayDate()} to ${summary.latest.toDisplayDate()}.",
            )
        },
        confirmButton = {
            TextButton({ onAction(WeightSyncPromptAction.DismissSummary) }) { Text("OK") }
        },
    )
}
