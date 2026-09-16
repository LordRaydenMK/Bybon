package dev.sanastasov.bybon.bodyweight.phase

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DietPhaseViewModelTest {

    private val today = LocalDate.of(2026, 9, 9)

    @Test
    fun `rejects a gain target below the start average`() = runTest {
        val viewModel = DietPhaseViewModel(
            officialAverageRepo(),
            backgroundScope,
            today,
        )

        val primed = viewModel.uiState.first { it.startWeightKg != null && it.canApply }
        assert(primed.kinds == listOf(null) + DietPhaseKind.entries)
        viewModel.onAction(DietPhaseEditorAction.OnKindSelected(DietPhaseKind.Gain))
        viewModel.onAction(DietPhaseEditorAction.OnWeeksChanged("8"))
        viewModel.onAction(DietPhaseEditorAction.OnTargetChanged("64.0"))

        val state = viewModel.uiState.first { it.error != null }
        assert(state.error == "Gain target must be above current weight")
        assert(!state.canApply)
    }

    @Test
    fun `apply persist a valid maintain phase`() = runTest {
        val repository = officialAverageRepo()
        val viewModel = DietPhaseViewModel(
            repository,
            backgroundScope,
            today,
        )
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.uiState.first { it.startWeightKg != null && it.canApply }
        viewModel.onAction(DietPhaseEditorAction.OnKindSelected(DietPhaseKind.Maintain))
        viewModel.onAction(DietPhaseEditorAction.OnTargetChanged("65.0"))
        viewModel.uiState.first {
            it.selectedKind == DietPhaseKind.Maintain && it.canApply && it.error == null
        }
        viewModel.onAction(DietPhaseEditorAction.OnApplyClicked)
        viewModel.effects.first { it == DietPhaseEditorEffect.NavigateBack }

        val open = repository.openPhase().first()
        assert(open?.phase is DietPhase.Maintain)
        assert(open?.phase?.targetWeight == BodyWeight.parseFromString("65.0"))
    }

    private fun officialAverageRepo(): FakeBodyWeightRepository {
        val weekStart = today.minusDays(2)
        return FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("65.0")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("65.0")),
                BodyWeightEntry(weekStart, BodyWeight.parseFromString("65.0")),
            ),
        )
    }
}
