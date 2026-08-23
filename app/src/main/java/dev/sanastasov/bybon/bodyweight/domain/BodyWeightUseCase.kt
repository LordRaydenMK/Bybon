package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.domain.weekOfYear
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.roundToInt

data class WeeklyAverageEntry(
    val weekOfYear: Int,
    val averageWeight: BodyWeight,
    val delta: BodyWeight?
)

data class BodyWeightDashboard(
    val thisWeekValues: List<BodyWeightEntry>?,
    val previousWeeksAverages: List<WeeklyAverageEntry>?
) {

    val thisWeekAverage: BodyWeight? = thisWeekValues?.averageWeight()?.weight

    val lastWeekAverage: BodyWeight? =
        thisWeekValues?.firstOrNull()?.date?.weekOfYear?.let { currentWeekNo ->
            previousWeeksAverages?.firstOrNull { it.weekOfYear == currentWeekNo - 1 }?.averageWeight
        }
}

fun BodyWeightRepository.bodyWeightDashboard(today: LocalDate): Flow<BodyWeightDashboard> =
    entries().map { allEntries ->
        val currentCw = today.weekOfYear

        val thisWeekValues = allEntries.takeWhile { entry ->
            entry.date.weekOfYear == currentCw
        }.map { BodyWeightEntry(it.date, it.weight) }

        val entriesByWeek = allEntries.filter { entry ->
            entry.date.weekOfYear in currentCw - 11..<currentCw
        }.groupBy { it.date.weekOfYear }

        val averagesByWeek = entriesByWeek.mapValues { entry ->
            entry.value.averageWeight()
        }

        val previousWeeksAverages = averagesByWeek.mapNotNull { (weekOfYear, averageWeight) ->
            if (averageWeight != null) {
                val delta = averagesByWeek[weekOfYear - 1]?.weight?.let { previousWeekAverage ->
                    averageWeight.weight - previousWeekAverage
                }
                WeeklyAverageEntry(weekOfYear, averageWeight.weight, delta)
            } else {
                null
            }
        }

        BodyWeightDashboard(
            thisWeekValues,
            previousWeeksAverages.takeIf { it.isNotEmpty() }
        )
    }

private fun List<BodyWeightEntry>.averageWeight(): BodyWeightEntry? =
    when {
        isEmpty() -> null
        size < 3 -> null
        else -> {
            val total = sumOf { it.weight.value }
            val average = total.toFloat() / size
            BodyWeightEntry(first().date, BodyWeight(average.roundToInt()))
        }
    }