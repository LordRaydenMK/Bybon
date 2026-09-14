package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WeightDashboardViewModelTest {

    private val today = LocalDate.of(2026, 9, 9)

    @Test
    fun `shows prominent log weight when today has no entry`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(today.minusDays(2), BodyWeight.parseFromString("64.8")),
                BodyWeightEntry(today.minusDays(3), BodyWeight.parseFromString("65.0")),
            ),
        )
        val viewModel = WeightDashboardViewModel(repository, backgroundScope, today)

        val state = viewModel.uiState.first { it.logWeightPrompt != LogWeightPrompt.Hidden }

        assert(state.logWeightPrompt == LogWeightPrompt.Prominent)
    }

    @Test
    fun `shows compact log weight when today already has an entry`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("64.8")),
                BodyWeightEntry(today.minusDays(2), BodyWeight.parseFromString("65.0")),
            ),
        )
        val viewModel = WeightDashboardViewModel(repository, backgroundScope, today)

        val state = viewModel.uiState.first { it.logWeightPrompt != LogWeightPrompt.Hidden }

        assert(state.logWeightPrompt == LogWeightPrompt.Compact)
    }

    @Test
    fun `uses current week average title when the week has enough entries`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("66.0")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("66.0")),
                BodyWeightEntry(today.minusDays(2), BodyWeight.parseFromString("66.0")),
            ),
        )
        val viewModel = WeightDashboardViewModel(repository, backgroundScope, today)

        val state = viewModel.uiState.first { it.logWeightPrompt != LogWeightPrompt.Hidden }

        assert(state.comparison?.title == "CW 37 average")
        assert(state.comparison?.currentWeightWeight == "66.0 kg")
    }

    @Test
    fun `falls back to last 7d average title when the current week is thin`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("64.8")),
                BodyWeightEntry(today.minusDays(5), BodyWeight.parseFromString("65.0")),
            ),
        )
        val viewModel = WeightDashboardViewModel(repository, backgroundScope, today)

        val state = viewModel.uiState.first { it.logWeightPrompt != LogWeightPrompt.Hidden }

        assert(state.comparison?.title == "Last 7d average")
        assert(state.comparison?.currentWeightWeight == "65.0 kg")
        assert(state.dailyHeaderText == "This week (8 Sep 2026) CW 37")
        assert(
            state.weeklyTrend?.single() == WeeklyTrendPointUi(
                "37",
                0,
                65.0f,
                isLastSevenDaysFallback = true,
            ),
        )
    }

    @Test
    fun `maps weekly trend points onto a calendar week axis`() = runTest {
        val weekStart = today.with(DayOfWeek.MONDAY)
        val repository = FakeBodyWeightRepository(
            officialWeek(weekStart, "66.0") +
                officialWeek(weekStart.minusWeeks(2), "64.0"),
        )
        val viewModel = WeightDashboardViewModel(repository, backgroundScope, today)

        val state = viewModel.uiState.first { it.logWeightPrompt != LogWeightPrompt.Hidden }

        assert(
            state.weeklyTrend == listOf(
                WeeklyTrendPointUi("35", 0, 64.0f),
                WeeklyTrendPointUi("37", 2, 66.0f),
            ),
        )
    }

    private fun officialWeek(weekStart: LocalDate, kilograms: String): List<BodyWeightEntry> =
        listOf(
            BodyWeightEntry(weekStart, BodyWeight.parseFromString(kilograms)),
            BodyWeightEntry(weekStart.plusDays(1), BodyWeight.parseFromString(kilograms)),
            BodyWeightEntry(weekStart.plusDays(2), BodyWeight.parseFromString(kilograms)),
        )
}
