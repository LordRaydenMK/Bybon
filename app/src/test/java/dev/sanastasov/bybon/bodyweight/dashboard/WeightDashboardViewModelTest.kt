package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WeightDashboardViewModelTest {

    private val today = LocalDate.of(2026, 9, 9)

    @Test
    fun `shows prominent log weight when today has no entry`() = runTest {
        val state = uiState(
            entry(today.minusDays(1), "65.2"),
            entry(today.minusDays(2), "64.8"),
            entry(today.minusDays(3), "65.0"),
        )

        assert(state.logWeightPrompt == LogWeightPrompt.Prominent)
    }

    @Test
    fun `shows compact log weight when today already has an entry`() = runTest {
        val state = uiState(
            entry(today, "65.2"),
            entry(today.minusDays(1), "64.8"),
            entry(today.minusDays(2), "65.0"),
        )

        assert(state.logWeightPrompt == LogWeightPrompt.Compact)
    }

    @Test
    fun `uses current week average title when the week has enough entries`() = runTest {
        val state = uiState(
            entry(today, "66.0"),
            entry(today.minusDays(1), "66.0"),
            entry(today.minusDays(2), "66.0"),
        )

        assert(state.comparison?.title == "CW 37 average")
        assert(state.comparison?.currentWeightWeight == "66.0 kg")
    }

    @Test
    fun `falls back to last 7d average title when the current week is thin`() = runTest {
        val state = uiState(
            entry(today, "65.2"),
            entry(today.minusDays(1), "64.8"),
            entry(today.minusDays(5), "65.0"),
        )

        assert(state.comparison?.title == "Last 7d average")
        assert(state.comparison?.currentWeightWeight == "65.0 kg")
        assert(state.dailyHeaderText == "This week (8 Sep 2026) CW 37")
    }

    private suspend fun TestScope.uiState(vararg entries: BodyWeightEntry): WeightDashboardUiState {
        val viewModel = WeightDashboardViewModel(
            FakeBodyWeightRepository(entries.toList()),
            backgroundScope,
            today,
        )
        return viewModel.uiState.first { it.logWeightPrompt != LogWeightPrompt.Hidden }
    }

    private fun entry(date: LocalDate, kg: String) =
        BodyWeightEntry(date, BodyWeight.parseFromString(kg))
}
