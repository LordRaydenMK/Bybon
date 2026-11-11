package dev.sanastasov.bybon.workout.domain

import java.time.LocalDate
import kotlin.time.Duration

data class LastSevenDaysSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val workoutCount: Int,
    val trainingTime: Duration,
    val workingSetCount: Int,
    val totalReps: Int,
    val performance: ExercisePerformanceChange,
    val musclesTrained: List<TrainedMuscle>,
)

data class ExercisePerformanceChange(
    val improved: Int,
    val maintained: Int,
    val declined: Int,
) {
    val comparableCount: Int get() = improved + maintained + declined
}

enum class TrainedMuscle {
    Chest,
    Back,
    Quads,
    Hamstrings,
    Glutes,
    Shoulders,
    Biceps,
    Triceps,
    Core,
    Calves,
    Forearms,
}

val TrainedMuscle.label: String
    get() = when (this) {
        TrainedMuscle.Quads -> "Quads"
        else -> name
    }

fun List<WorkoutSession>.lastSevenDaysSummary(today: LocalDate): LastSevenDaysSummary {
    val startDate = today.minusDays(6)
    val completed = filter { it.state is WorkoutState.Completed }
    val inWindow = completed.filter { session ->
        session.startedAt.toLocalDate() in startDate..today
    }
    val beforeWindow = completed.filter { session ->
        session.startedAt.toLocalDate() < startDate
    }
    return LastSevenDaysSummary(
        startDate = startDate,
        endDate = today,
        workoutCount = inWindow.size,
        trainingTime = inWindow.totalTrainingTime(),
        workingSetCount = inWindow.sumOf { session ->
            session.exercises.sumOf { it.sets.size }
        },
        totalReps = inWindow.sumOf { session ->
            session.exercises.sumOf { exercise -> exercise.sets.sumOf { it.reps } }
        },
        performance = performanceChange(inWindow, beforeWindow),
        musclesTrained = inWindow.musclesTrained(),
    )
}

private fun List<WorkoutSession>.totalTrainingTime(): Duration =
    fold(Duration.ZERO) { total, session ->
        total + (session.duration ?: Duration.ZERO)
    }

private fun performanceChange(
    inWindow: List<WorkoutSession>,
    beforeWindow: List<WorkoutSession>,
): ExercisePerformanceChange {
    var improved = 0
    var maintained = 0
    var declined = 0
    inWindow.exerciseIds().forEach { exerciseId ->
        val current = inWindow.bestTopSetOneRm(exerciseId) ?: return@forEach
        val previous = beforeWindow.latestTopSetOneRm(exerciseId) ?: return@forEach
        when {
            current > previous -> improved++
            current < previous -> declined++
            else -> maintained++
        }
    }
    return ExercisePerformanceChange(improved, maintained, declined)
}

private fun List<WorkoutSession>.exerciseIds(): Set<String> =
    flatMap { session -> session.exercises.map { it.id } }.toSet()

private fun List<WorkoutSession>.bestTopSetOneRm(exerciseId: String): Weight? =
    mapNotNull { session -> session.exerciseOrNull(exerciseId)?.topSet?.oneRm }.maxOrNull()

private fun List<WorkoutSession>.latestTopSetOneRm(exerciseId: String): Weight? =
    filter { session -> session.exerciseOrNull(exerciseId) != null }
        .maxByOrNull { it.startedAt }
        ?.exerciseOrNull(exerciseId)
        ?.topSet
        ?.oneRm

private fun WorkoutSession.exerciseOrNull(exerciseId: String): WorkoutExercise? =
    exercises.firstOrNull { it.id == exerciseId }

private fun List<WorkoutSession>.musclesTrained(): List<TrainedMuscle> =
    flatMap { session -> session.exercises.flatMap { it.exerciseDefinition.trainedMuscles() } }
        .toSet()
        .sortedBy { it.ordinal }

internal fun ExerciseDefinition.trainedMuscles(): Set<TrainedMuscle> {
    val fromPrimary = primaryMuscles.mapNotNull { it.toTrainedMuscle() }.toSet()
    if (fromPrimary.isNotEmpty()) return fromPrimary
    return bodyPart?.toTrainedMuscles().orEmpty()
}

internal fun String.toTrainedMuscle(): TrainedMuscle? = PRIMARY_MUSCLE_TO_TRAINED[this]

private fun BodyPart.toTrainedMuscles(): Set<TrainedMuscle> = when (this) {
    BodyPart.Chest -> setOf(TrainedMuscle.Chest)
    BodyPart.Back -> setOf(TrainedMuscle.Back)
    BodyPart.UpperLegs -> setOf(TrainedMuscle.Quads)
    BodyPart.LowerLegs -> setOf(TrainedMuscle.Calves)
    BodyPart.Shoulders -> setOf(TrainedMuscle.Shoulders)
    BodyPart.Core -> setOf(TrainedMuscle.Core)
    BodyPart.LowerArms -> setOf(TrainedMuscle.Forearms)
    BodyPart.UpperArms, BodyPart.FullBody -> emptySet()
}

private val PRIMARY_MUSCLE_TO_TRAINED = mapOf(
    "pectoralis_major" to TrainedMuscle.Chest,
    "pectoralis_minor" to TrainedMuscle.Chest,
    "latissimus_dorsi" to TrainedMuscle.Back,
    "rhomboids" to TrainedMuscle.Back,
    "trapezius" to TrainedMuscle.Back,
    "erector_spinae" to TrainedMuscle.Back,
    "teres_major" to TrainedMuscle.Back,
    "teres_minor" to TrainedMuscle.Back,
    "infraspinatus" to TrainedMuscle.Back,
    "supraspinatus" to TrainedMuscle.Back,
    "subscapularis" to TrainedMuscle.Back,
    "levator_scapulae" to TrainedMuscle.Back,
    "quadriceps" to TrainedMuscle.Quads,
    "hamstrings" to TrainedMuscle.Hamstrings,
    "gluteus_maximus" to TrainedMuscle.Glutes,
    "gluteus_medius" to TrainedMuscle.Glutes,
    "gluteus_minimus" to TrainedMuscle.Glutes,
    "anterior_deltoid" to TrainedMuscle.Shoulders,
    "lateral_deltoid" to TrainedMuscle.Shoulders,
    "posterior_deltoid" to TrainedMuscle.Shoulders,
    "deltoid" to TrainedMuscle.Shoulders,
    "biceps_brachii" to TrainedMuscle.Biceps,
    "brachialis" to TrainedMuscle.Biceps,
    "triceps_brachii" to TrainedMuscle.Triceps,
    "anconeus" to TrainedMuscle.Triceps,
    "rectus_abdominis" to TrainedMuscle.Core,
    "obliques" to TrainedMuscle.Core,
    "transversus_abdominis" to TrainedMuscle.Core,
    "transverse_abdominis" to TrainedMuscle.Core,
    "serratus_anterior" to TrainedMuscle.Core,
    "gastrocnemius" to TrainedMuscle.Calves,
    "soleus" to TrainedMuscle.Calves,
    "calves" to TrainedMuscle.Calves,
    "tibialis_anterior" to TrainedMuscle.Calves,
    "wrist_flexors" to TrainedMuscle.Forearms,
    "wrist_extensors" to TrainedMuscle.Forearms,
    "brachioradialis" to TrainedMuscle.Forearms,
    "forearm" to TrainedMuscle.Forearms,
    "forearms" to TrainedMuscle.Forearms,
    "forearm_flexors" to TrainedMuscle.Forearms,
    "forearm_extensors" to TrainedMuscle.Forearms,
)
