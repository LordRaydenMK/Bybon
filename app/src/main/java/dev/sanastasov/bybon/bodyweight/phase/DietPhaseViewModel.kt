package dev.sanastasov.bybon.bodyweight.phase

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseValidation
import dev.sanastasov.bybon.bodyweight.domain.EffectiveDietPhase
import dev.sanastasov.bybon.bodyweight.domain.bodyWeightDashboard
import dev.sanastasov.bybon.bodyweight.domain.durationWeeksOrNull
import dev.sanastasov.bybon.bodyweight.domain.endExclusive
import dev.sanastasov.bybon.bodyweight.domain.kind
import dev.sanastasov.bybon.bodyweight.domain.plannedRatePerWeek
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
                weeks.value = phase.durationWeeksOrNull?.toString().orEmpty()
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
        val kind = selectedKind.value
        val phase = when (kind) {
            null -> null

            else -> when (val result = createPhase(kind, start)) {
                is DietPhaseValidation.Valid -> result.phase
                is DietPhaseValidation.Invalid -> return
            }
        }
        coroutineScope.launch {
            repository.openPhase().first()?.let { open ->
                repository.endPhase(open, today)
            }
            phase?.let { created ->
                repository.updatePhase(DietPhaseRecord(0, created))
            }
            _effects.send(DietPhaseEditorEffect.NavigateBack)
        }
    }

    private fun editorUi(
        kind: DietPhaseKind?,
        weeksText: String,
        targetText: String,
        start: BodyWeight?,
        isPrimed: Boolean,
    ): DietPhaseEditorUi {
        val result = start?.let { weight ->
            kind?.let { createPhase(it, weight, weeksText, targetText) }
        }
        val phase = (result as? DietPhaseValidation.Valid)?.phase
        return DietPhaseEditorUi(
            selectedKind = kind,
            weeks = weeksText,
            targetKg = targetText,
            startWeightKg = start?.kilograms?.toString(),
            rateCaption = if (start != null && phase?.endExclusive() != null) {
                phase.plannedRatePerWeek().formatRate(start)
            } else {
                null
            },
            error = (result as? DietPhaseValidation.Invalid)?.message,
            canApply = isPrimed &&
                start != null &&
                (kind == null || result is DietPhaseValidation.Valid),
        )
    }

    private fun createPhase(
        kind: DietPhaseKind,
        start: BodyWeight,
        weeksText: String = weeks.value,
        targetText: String = targetKg.value,
    ): DietPhaseValidation = DietPhase.create(
        kind,
        today.isoWeekStart(),
        start,
        parseWeight(targetText),
        weeksText.toIntOrNull(),
    )

    private fun parseWeight(value: String): BodyWeight? = try {
        BodyWeight.parseFromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}
