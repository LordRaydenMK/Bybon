package dev.sanastasov.bybon.bodyweight.phase

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightDashboard
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DietPhaseViewModel(
    private val repository: BodyWeightRepository,
    private val coroutineScope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
) {

    private val actions = MutableSharedFlow<DietPhaseEditorAction>(extraBufferCapacity = 32)
    private val _effects = Channel<DietPhaseEditorEffect>(Channel.BUFFERED)
    val effects: Flow<DietPhaseEditorEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<DietPhaseEditorUi> = repository.bodyWeightDashboard(today)
        .map(::seedFields)
        .distinctUntilChanged()
        .flatMapLatest { seed ->
            actions.scan(seed, ::reduce)
        }
        .map(::toEditorUi)
        .stateInWhileInForeground(coroutineScope, DietPhaseEditorUi())

    fun onAction(action: DietPhaseEditorAction) {
        when (action) {
            DietPhaseEditorAction.OnBackClicked ->
                _effects.trySend(DietPhaseEditorEffect.NavigateBack)

            DietPhaseEditorAction.OnApplyClicked -> apply()

            else -> {
                if (!actions.tryEmit(action)) {
                    coroutineScope.launch { actions.emit(action) }
                }
            }
        }
    }

    private fun apply() {
        val fields = uiState.value.toFields()
        val start = fields.startWeight ?: return
        val phase = when (val kind = fields.selectedKind) {
            null -> null

            else -> when (val result = createPhase(kind, start, fields.weeks, fields.targetKg)) {
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

    private fun seedFields(dashboard: BodyWeightDashboard): DietPhaseEditorFields {
        val start = dashboard.currentAverage
        return when (val effective = dashboard.effectivePhase) {
            is EffectiveDietPhase.On -> {
                val phase = effective.phase
                DietPhaseEditorFields(
                    selectedKind = phase.kind,
                    weeks = phase.durationWeeksOrNull?.toString().orEmpty(),
                    targetKg = phase.targetWeight.kilograms.toString(),
                    startWeight = start,
                )
            }

            EffectiveDietPhase.Off -> DietPhaseEditorFields(
                selectedKind = null,
                weeks = "",
                targetKg = start?.kilograms?.toString().orEmpty(),
                startWeight = start,
            )
        }
    }

    private fun reduce(
        fields: DietPhaseEditorFields,
        action: DietPhaseEditorAction,
    ): DietPhaseEditorFields = when (action) {
        is DietPhaseEditorAction.OnKindSelected -> fields.copy(selectedKind = action.kind)

        is DietPhaseEditorAction.OnWeeksChanged -> fields.copy(weeks = action.weeks)

        is DietPhaseEditorAction.OnTargetChanged -> fields.copy(targetKg = action.targetKg)

        DietPhaseEditorAction.OnBackClicked,
        DietPhaseEditorAction.OnApplyClicked,
        -> fields
    }

    private fun toEditorUi(fields: DietPhaseEditorFields): DietPhaseEditorUi {
        val start = fields.startWeight
        val result = start?.let { weight ->
            fields.selectedKind?.let { kind ->
                createPhase(kind, weight, fields.weeks, fields.targetKg)
            }
        }
        val phase = (result as? DietPhaseValidation.Valid)?.phase
        return DietPhaseEditorUi(
            kinds = listOf(null) + DietPhaseKind.entries,
            selectedKind = fields.selectedKind,
            weeks = fields.weeks,
            targetKg = fields.targetKg,
            startWeightKg = start?.kilograms?.toString(),
            rateCaption = if (start != null && phase?.endExclusive() != null) {
                phase.plannedRatePerWeek().formatRate(start)
            } else {
                null
            },
            error = (result as? DietPhaseValidation.Invalid)?.message,
            canApply = start != null &&
                (fields.selectedKind == null || result is DietPhaseValidation.Valid),
        )
    }

    private fun DietPhaseEditorUi.toFields(): DietPhaseEditorFields = DietPhaseEditorFields(
        selectedKind = selectedKind,
        weeks = weeks,
        targetKg = targetKg,
        startWeight = startWeightKg?.let(::parseWeight),
    )

    private fun createPhase(
        kind: DietPhaseKind,
        start: BodyWeight,
        weeksText: String,
        targetText: String,
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
