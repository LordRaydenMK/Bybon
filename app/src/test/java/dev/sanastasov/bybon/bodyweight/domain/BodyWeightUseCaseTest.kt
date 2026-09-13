package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BodyWeightUseCaseTest {

    private val today = LocalDate.of(2026, 9, 9)

    @Test
    fun `current week average requires three entries`() = runTest {
        val dashboard = dashboard(
            entry(today, "65.2"),
            entry(today.minusDays(1), "64.8"),
        )

        assert(dashboard.thisWeekAverage == null)
        assert(dashboard.lastSevenDaysAverage == null)
    }

    @Test
    fun `last seven days average is used when current week has fewer than three entries`() = runTest {
        val dashboard = dashboard(
            entry(today, "65.2"),
            entry(today.minusDays(1), "64.8"),
            entry(today.minusDays(5), "65.0"),
        )

        assert(dashboard.thisWeekValues?.size == 2)
        assert(dashboard.thisWeekAverage == null)
        assert(dashboard.lastSevenDaysAverage == BodyWeight.parseFromString("65.0"))
    }

    @Test
    fun `current week average takes priority when it has three entries`() = runTest {
        val dashboard = dashboard(
            entry(today, "66.0"),
            entry(today.minusDays(1), "66.0"),
            entry(today.minusDays(2), "66.0"),
            entry(today.minusDays(5), "64.0"),
        )

        assert(dashboard.thisWeekAverage == BodyWeight.parseFromString("66.0"))
        assert(dashboard.lastSevenDaysAverage == BodyWeight.parseFromString("65.5"))
    }

    @Test
    fun `entries older than seven days are ignored for last seven days average`() = runTest {
        val dashboard = dashboard(
            entry(today, "65.2"),
            entry(today.minusDays(1), "64.8"),
            entry(today.minusDays(7), "65.0"),
        )

        assert(dashboard.lastSevenDaysAverage == null)
    }

    private suspend fun dashboard(vararg entries: BodyWeightEntry) =
        FakeBodyWeightRepository(entries.toList())
            .bodyWeightDashboard(today)
            .first()

    private fun entry(date: LocalDate, kg: String) =
        BodyWeightEntry(date, BodyWeight.parseFromString(kg))
}
