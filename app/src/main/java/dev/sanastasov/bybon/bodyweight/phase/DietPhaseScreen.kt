@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.bodyweight.phase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.ui.components.NumberInputField

@Composable
fun BodyWeightModule.DietPhaseScreen(onNavigateBack: () -> Unit) {
    val viewModel = retain {
        DietPhaseViewModel(bodyWeightRepository, it.coroutineScope)
    }
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            DietPhaseEditorEffect.NavigateBack -> onNavigateBack()
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DietPhaseEditorContent(uiState, viewModel::onAction)
}

@Composable
private fun DietPhaseEditorContent(
    state: DietPhaseEditorUi,
    onAction: (DietPhaseEditorAction) -> Unit,
) {
    val kinds = listOf(null) + DietPhaseKind.entries
    Scaffold(
        topBar = {
            BybonTopAppBar("Diet phase", { onAction(DietPhaseEditorAction.OnBackClicked) })
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhaseKindSelector(state.selectedKind, kinds, onAction)
            StartAverageLabel(state.startWeightKg)

            if (state.selectedKind == DietPhaseKind.Maintain) {
                TargetRow(state, onAction)
            }

            if (
                state.selectedKind == DietPhaseKind.Gain ||
                state.selectedKind == DietPhaseKind.Lose
            ) {
                GainLoseFields(state, onAction)
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { onAction(DietPhaseEditorAction.OnApplyClicked) },
                enabled = state.canApply,
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun PhaseKindSelector(
    selectedKind: DietPhaseKind?,
    kinds: List<DietPhaseKind?>,
    onAction: (DietPhaseEditorAction) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        kinds.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = selectedKind == kind,
                onClick = { onAction(DietPhaseEditorAction.OnKindSelected(kind)) },
                shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                icon = {},
                label = { Text(kind?.name ?: "None") },
            )
        }
    }
}

@Composable
private fun StartAverageLabel(startWeightKg: String?) {
    startWeightKg?.let { start ->
        Text(
            "Start average $start kg",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GainLoseFields(state: DietPhaseEditorUi, onAction: (DietPhaseEditorAction) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Weeks")
        NumberInputField("diet-phase-weeks", state.weeks) {
            onAction(DietPhaseEditorAction.OnWeeksChanged(it))
        }
    }
    TargetRow(state, onAction)
    state.rateCaption?.let { caption ->
        Text(caption, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TargetRow(state: DietPhaseEditorUi, onAction: (DietPhaseEditorAction) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Target")
        NumberInputField("diet-phase-target-${state.selectedKind}", state.targetKg) {
            onAction(DietPhaseEditorAction.OnTargetChanged(it))
        }
        Text("kg")
    }
}

@Preview
@Composable
private fun DietPhaseEditorGainPreview() {
    DietPhaseEditorContent(
        DietPhaseEditorUi(
            selectedKind = DietPhaseKind.Gain,
            weeks = "12",
            targetKg = "67.0",
            startWeightKg = "65.0",
            rateCaption = "+0.15 kg/week (0.23%)",
            canApply = true,
        ),
        {},
    )
}

@Preview
@Composable
private fun DietPhaseEditorInvalidPreview() {
    DietPhaseEditorContent(
        DietPhaseEditorUi(
            selectedKind = DietPhaseKind.Gain,
            weeks = "4",
            targetKg = "62.0",
            startWeightKg = "65.0",
            error = "Gain target must be above current weight",
        ),
        {},
    )
}
