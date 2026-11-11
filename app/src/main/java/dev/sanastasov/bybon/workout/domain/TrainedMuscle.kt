package dev.sanastasov.bybon.workout.domain

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
