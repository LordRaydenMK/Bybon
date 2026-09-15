package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import dev.sanastasov.bybon.domain.isoWeekStart
import dev.sanastasov.bybon.domain.weekOfYear
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

    @Test
    fun `weekly trend uses official current week average when it has three entries`() = runTest {
        val weekStart = today.isoWeekStart()
        val repository = FakeBodyWeightRepository(
            officialWeek(weekStart, "66.0") +
                officialWeek(weekStart.minusWeeks(1), "65.0"),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(
            dashboard.weeklyTrend?.last() == WeeklyTrendPoint(
                weekStart,
                37,
                BodyWeight.parseFromString("66.0"),
            ),
        )
        assert(dashboard.weeklyTrend?.first()?.weekStart == weekStart.minusWeeks(1))
        assert(
            dashboard.weeklyTrend?.none { point ->
                point.isLastSevenDaysFallback
            } == true,
        )
    }

    @Test
    fun `weekly trend uses last 7d average for a thin current week`() = runTest {
        val repository = FakeBodyWeightRepository(
            listOf(
                BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(today.minusDays(1), BodyWeight.parseFromString("64.8")),
                BodyWeightEntry(today.minusDays(5), BodyWeight.parseFromString("65.0")),
            ),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        val currentWeekPoint = dashboard.weeklyTrend?.single()
        assert(currentWeekPoint?.weekStart == today.isoWeekStart())
        assert(currentWeekPoint?.averageWeight == BodyWeight.parseFromString("65.0"))
        assert(currentWeekPoint?.isLastSevenDaysFallback == true)
    }

    @Test
    fun `weekly trend omits thin past weeks and keeps calendar gaps`() = runTest {
        val weekStart = today.isoWeekStart()
        val repository = FakeBodyWeightRepository(
            officialWeek(weekStart, "66.0") +
                officialWeek(weekStart.minusWeeks(3), "64.0") +
                listOf(
                    BodyWeightEntry(
                        weekStart.minusWeeks(1),
                        BodyWeight.parseFromString("65.0"),
                    ),
                    BodyWeightEntry(
                        weekStart.minusWeeks(1).plusDays(1),
                        BodyWeight.parseFromString("65.0"),
                    ),
                ),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(
            dashboard.weeklyTrend?.map { point -> point.weekStart } == listOf(
                weekStart.minusWeeks(3),
                weekStart,
            ),
        )
        assert(
            dashboard.previousWeeksAverages?.map { entry -> entry.weekOfYear } == listOf(
                weekStart.minusWeeks(3).weekOfYear,
            ),
        )
    }

    @Test
    fun `weekly trend shows at most the last 16 weeks`() = runTest {
        val weekStart = today.isoWeekStart()
        val entries = (0..19).flatMap { weeksAgo ->
            officialWeek(weekStart.minusWeeks(weeksAgo.toLong()), "${80 - weeksAgo}.0")
        }
        val repository = FakeBodyWeightRepository(entries)

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(dashboard.weeklyTrend?.size == 16)
        assert(dashboard.weeklyTrend?.first()?.weekStart == weekStart.minusWeeks(15))
        assert(dashboard.weeklyTrend?.last()?.weekStart == weekStart)
        assert(dashboard.previousWeeksAverages?.size == 15)
        assert(
            dashboard.previousWeeksAverages?.none { entry ->
                entry.weekOfYear == weekStart.weekOfYear
            } == true,
        )
    }

    @Test
    fun `weekly trend includes weeks from the previous year`() = runTest {
        val today = LocalDate.of(2026, 1, 14)
        val previousYearWeekStart = LocalDate.of(2025, 12, 22)
        val repository = FakeBodyWeightRepository(
            officialWeek(today.isoWeekStart(), "68.0") +
                officialWeek(previousYearWeekStart, "70.0"),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(
            dashboard.weeklyTrend?.any { point ->
                point.weekStart == previousYearWeekStart
            } == true,
        )
        assert(
            dashboard.previousWeeksAverages?.any { entry ->
                entry.weekOfYear == previousYearWeekStart.weekOfYear
            } == true,
        )
    }

    @Test
    fun `dashboard combines entries with the open diet phase`() = runTest {
        val weekStart = today.isoWeekStart()
        val startWeight = BodyWeight.parseFromString("65.0")
        val repository = FakeBodyWeightRepository(
            officialWeek(weekStart, "65.0") + officialWeek(weekStart.minusWeeks(1), "64.8"),
            DietPhaseRecord(
                1,
                DietPhase.create(
                    DietPhaseKind.Maintain,
                    weekStart,
                    startWeight,
                    startWeight,
                ).requireValid(),
            ),
        )

        val dashboard = repository.bodyWeightDashboard(today).first()

        assert(dashboard.dietPhaseIsMaintain())
        assert(dashboard.onTrack == true)
        assert(dashboard.weeklyTrend?.none { it.isLastSevenDaysFallback } == true)
    }

    private fun BodyWeightDashboard.dietPhaseIsMaintain(): Boolean =
        (effectivePhase as? EffectiveDietPhase.On)?.phase is DietPhase.Maintain

    private fun officialWeek(weekStart: LocalDate, kilograms: String): List<BodyWeightEntry> =
        listOf(
            BodyWeightEntry(weekStart, BodyWeight.parseFromString(kilograms)),
            BodyWeightEntry(weekStart.plusDays(1), BodyWeight.parseFromString(kilograms)),
            BodyWeightEntry(weekStart.plusDays(2), BodyWeight.parseFromString(kilograms)),
        )
}
