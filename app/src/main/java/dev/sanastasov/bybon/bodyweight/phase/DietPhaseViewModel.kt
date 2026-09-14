package dev.sanastasov.bybon.bodyweight.phase

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseValidation
import dev.sanastasov.bybon.bodyweight.domain.EffectiveDietPhase
import dev.sanastasov.bybon.bodyweight.domain.bodyWeightDashboard
import dev.sanastasov.bybon.bodyweight.domain.validateDietPhase
import dev.sanastasov.bybon.domain.isoWeekStart
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DietPhaseViewModel(
    private val repository: BodyWeightRepository,
    private val coroutineScope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
) {

    private val _effects = Channel<DietPhaseEditorEffect>(Channel.BUFFERED)
    val effects: Flow<DietPhaseEditorEffect> = _effects.receiveAsFlow()

    private val selectedKind = MutableStateFlow<DietPhaseKind?>(null)
    private val weeks = MutableStateFlow("")
    private val targetKg = MutableStateFlow("")
    private val startWeight = MutableStateFlow<BodyWeight?>(null)
    private val primed = MutableStateFlow(false)

    val uiState: StateFlow<DietPhaseEditorUi> = combine(
        selectedKind,
        weeks,
        targetKg,
        startWeight,
        primed,
    ) { kind, weeksText, targetText, start, isPrimed ->
        editorUi(kind, weeksText, targetText, start, isPrimed)
    }.stateInWhileInForeground(coroutineScope, DietPhaseEditorUi())

    init {
        coroutineScope.launch { primeEditor() }
    }

    fun onAction(action: DietPhaseEditorAction) {
        when (action) {
            DietPhaseEditorAction.OnBackClicked ->
                _effects.trySend(DietPhaseEditorEffect.NavigateBack)

            is DietPhaseEditorAction.OnKindSelected -> selectedKind.value = action.kind

            is DietPhaseEditorAction.OnWeeksChanged -> weeks.value = action.weeks

            is DietPhaseEditorAction.OnTargetChanged -> targetKg.value = action.targetKg

            DietPhaseEditorAction.OnApplyClicked -> apply()
        }
    }

    private suspend fun primeEditor() {
        val dashboard = repository.bodyWeightDashboard(today).first()
        val start = dashboard.currentAverage
        when (val effective = dashboard.effectivePhase) {
            is EffectiveDietPhase.On -> {
                val phase = effective.phase
                selectedKind.value = phase.kind
                weeks.value = phase.durationWeeks?.toString().orEmpty()
                targetKg.value = phase.targetWeight.kilograms.toString()
            }

            EffectiveDietPhase.Off -> {
                selectedKind.value = null
                targetKg.value = start?.kilograms?.toString().orEmpty()
            }
        }
        startWeight.value = start
        primed.value = true
    }

    private fun apply() {
        val start = startWeight.value ?: return
        val validation = validateCurrent(start)
        coroutineScope.launch {
            when (validation) {
                is DietPhaseValidation.Valid -> {
                    if (validation.phase == null) {
                        repository.clear()
                    } else {
                        repository.apply(validation.phase)
                    }
                    _effects.send(DietPhaseEditorEffect.NavigateBack)
                }

                is DietPhaseValidation.Invalid -> Unit
            }
        }
    }

    private fun editorUi(
        kind: DietPhaseKind?,
        weeksText: String,
        targetText: String,
        start: BodyWeight?,
        isPrimed: Boolean,
    ): DietPhaseEditorUi {
        val validation = start?.let { validateCurrent(it, kind, weeksText, targetText) }
        val preview = (validation as? DietPhaseValidation.Valid)?.preview
        return DietPhaseEditorUi(
            selectedKind = kind,
            weeks = weeksText,
            targetKg = targetText,
            startWeightKg = start?.kilograms?.toString(),
            rateCaption = if (start != null && preview != null) {
                preview.ratePerWeek.formatRate(start)
            } else {
                null
            },
            error = (validation as? DietPhaseValidation.Invalid)?.message,
            canApply = isPrimed && validation is DietPhaseValidation.Valid,
        )
    }

    private fun validateCurrent(
        start: BodyWeight,
        kind: DietPhaseKind? = selectedKind.value,
        weeksText: String = weeks.value,
        targetText: String = targetKg.value,
    ): DietPhaseValidation {
        val parsedWeeks = weeksText.toIntOrNull()
        val parsedTarget = parseWeight(targetText)
        return validateDietPhase(
            kind,
            today.isoWeekStart(),
            start,
            parsedTarget,
            parsedWeeks,
        )
    }

    private fun parseWeight(value: String): BodyWeight? = try {
        BodyWeight.parseFromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}
