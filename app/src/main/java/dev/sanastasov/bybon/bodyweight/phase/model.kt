package dev.sanastasov.bybon.bodyweight.phase

import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind

data class DietPhaseEditorUi(
    val selectedKind: DietPhaseKind? = null,
    val weeks: String = "",
    val targetKg: String = "",
    val startWeightKg: String? = null,
    val rateCaption: String? = null,
    val error: String? = null,
    val canApply: Boolean = false,
)

sealed class DietPhaseEditorAction {
    data object OnBackClicked : DietPhaseEditorAction()
    data class OnKindSelected(
        val kind: DietPhaseKind?,
    ) : DietPhaseEditorAction()
    data class OnWeeksChanged(
        val weeks: String,
    ) : DietPhaseEditorAction()
    data class OnTargetChanged(
        val targetKg: String,
    ) : DietPhaseEditorAction()
    data object OnApplyClicked : DietPhaseEditorAction()
}

sealed class DietPhaseEditorEffect {
    data object NavigateBack : DietPhaseEditorEffect()
}
