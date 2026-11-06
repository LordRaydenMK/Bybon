package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.domain.toDisplayDateWithRelative
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.ExercisePerformance
import dev.sanastasov.bybon.workout.domain.ExerciseSessionLog
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.PersonalRecord
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.label
import dev.sanastasov.bybon.workout.domain.toCatalogLabel
import dev.sanastasov.bybon.workout.ui.formatLoadedSet
import dev.sanastasov.bybon.workout.ui.toLoadLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ExerciseDetailsUiState {
    data object Loading : ExerciseDetailsUiState()
    data class Content(
        val name: String,
        val images: ExerciseImages? = null,
        val facts: List<String> = emptyList(),
        val personalRecord: PersonalRecordUi? = null,
        val oneRmTrend: List<ExerciseOneRmPointUi> = emptyList(),
        val recentSessions: List<ExerciseDetailsSessionUi> = emptyList(),
        val primaryMuscles: List<String> = emptyList(),
        val secondaryMuscles: List<String> = emptyList(),
        val description: String? = null,
        val instructions: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
    ) : ExerciseDetailsUiState()
}

data class PersonalRecordUi(
    val oneRm: Weight,
    val dateLabel: String,
)

data class ExerciseOneRmPointUi(
    val dateLabel: String,
    val kilograms: Float,
    val oneRm: Weight,
    val weightKg: String,
    val reps: Int,
    val averageKilograms: Float? = null,
    val averageOneRm: Weight? = null,
)

data class ExerciseDetailsSessionUi(
    val dateLabel: String,
    val planName: String,
    val sets: List<ExerciseDetailsSetUi>,
)

data class ExerciseDetailsSetUi(
    val number: Int,
    val weightKg: String,
    val reps: Int,
    val oneRm: Weight?,
)

internal fun ExerciseDefinition.toDetailsUi(
    performance: ExercisePerformance = ExercisePerformance(null, emptyList()),
    now: LocalDateTime = LocalDateTime.now(),
): ExerciseDetailsUiState.Content = ExerciseDetailsUiState.Content(
    name = name,
    images = images,
    facts = listOfNotNull(
        bodyPart?.label,
        equipment?.label,
        mechanic?.name,
        forceType?.name,
        difficulty?.name,
    ),
    personalRecord = performance.personalRecord?.toPersonalRecordUi(now.toLocalDate()),
    oneRmTrend = performance.recentSessions.toOneRmTrend(),
    recentSessions = performance.recentSessions.map { it.toSessionUi() },
    primaryMuscles = primaryMuscles.map { it.toCatalogLabel() },
    secondaryMuscles = secondaryMuscles.map { it.toCatalogLabel() },
    description = description,
    instructions = instructions.orEmpty(),
    tips = tips.orEmpty(),
    tags = tags.map { it.toCatalogLabel() },
)

private val CHART_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.US)

internal fun PersonalRecord.toPersonalRecordUi(today: LocalDate? = null): PersonalRecordUi {
    val date = achievedAt.toLocalDate()
    return PersonalRecordUi(
        oneRm = oneRm,
        dateLabel = if (today == null) {
            date.toDisplayDate()
        } else {
            date.toDisplayDateWithRelative(today)
        },
    )
}

internal fun List<ExerciseSessionLog>.toOneRmTrend(): List<ExerciseOneRmPointUi> =
    asReversed().mapNotNull { session ->
        val oneRm = session.topSetOneRm ?: return@mapNotNull null
        val averageOneRm = session.averageWorkSetOneRm
        ExerciseOneRmPointUi(
            dateLabel = CHART_DATE_FORMAT.format(session.startedAt),
            kilograms = oneRm.kilogramsValue,
            oneRm = oneRm,
            weightKg = session.topSet.weight.toLoadLabel(),
            reps = session.topSet.reps,
            averageKilograms = averageOneRm?.kilogramsValue,
            averageOneRm = averageOneRm,
        )
    }

internal fun oneRmTrendPopupText(points: List<ExerciseOneRmPointUi>, index: Int): String {
    val point = points.getOrNull(index) ?: return ""
    val load = "${point.dateLabel}  ${formatLoadedSet(point.weightKg, point.reps)}"
    val average = point.averageOneRm?.let { " · avg ${it.kilograms} kg" }.orEmpty()
    return load + average
}

private fun ExerciseSessionLog.toSessionUi(): ExerciseDetailsSessionUi = ExerciseDetailsSessionUi(
    dateLabel = startedAt.toLocalDate().toDisplayDate(),
    planName = planName,
    sets = workSets.mapIndexed { index, set -> set.toSetUi(index + 1) },
)

private fun ExerciseSet.toSetUi(number: Int): ExerciseDetailsSetUi = ExerciseDetailsSetUi(
    number = number,
    weightKg = weight.toLoadLabel(),
    reps = reps,
    oneRm = oneRm,
)
