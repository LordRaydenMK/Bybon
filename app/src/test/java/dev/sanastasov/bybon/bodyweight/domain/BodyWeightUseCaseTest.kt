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
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("64.8")),
            ),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(dashboard.thisWeekAverage == null)
        assert(dashboard.lastSevenDaysAverage == null)
    }

    @Test
    fun `last seven days average is used when current week has fewer than three entries`() =
        runTest {
            val repository = FakeBodyWeightRepository(
                listOf(
                    BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
                    BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("64.8")),
                    BodyWeightEntry(today.minusDays(5), BodyWeight.parseFromString("65.0")),
                ),
            )

            val dashboard = repository.bodyWeightDashboard(today).first()

            assert(dashboard.thisWeekValues?.size == 2)
            assert(dashboard.thisWeekAverage == null)
            assert(dashboard.lastSevenDaysAverage == BodyWeight.parseFromString("65.0"))
        }

    @Test
    fun `current week average takes priority when it has three entries`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("66.0")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("66.0")),
                BodyWeightEntry(today.minusDays(2), BodyWeight.parseFromString("66.0")),
                BodyWeightEntry(today.minusDays(5), BodyWeight.parseFromString("64.0")),
            ),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(dashboard.thisWeekAverage == BodyWeight.parseFromString("66.0"))
        assert(dashboard.lastSevenDaysAverage == BodyWeight.parseFromString("65.5"))
    }

    @Test
    fun `entries older than seven days are ignored for last seven days average`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("64.8")),
                BodyWeightEntry(today.minusDays(7), BodyWeight.parseFromString("65.0")),
            ),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(dashboard.lastSevenDaysAverage == null)
    }
}
