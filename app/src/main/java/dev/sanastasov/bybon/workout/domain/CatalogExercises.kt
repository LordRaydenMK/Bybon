package dev.sanastasov.bybon.workout.domain

internal val bybonCatalogExercises = listOf(
    ExerciseDefinition(
        "iso-lat-row",
        "Iso-Lateral Row (machine)",
        BodyPart.Back,
        Equipment.Machine,
        Mechanic.Compound,
    ),
    ExerciseDefinition(
        "squat-machine",
        "Squat (machine)",
        BodyPart.UpperLegs,
        Equipment.Machine,
        Mechanic.Compound,
    ),
    ExerciseDefinition(
        "triceps-press-machine",
        "Triceps Press (machine)",
        BodyPart.UpperArms,
        Equipment.Machine,
        Mechanic.Isolation,
    ),
    ExerciseDefinition(
        "lateral-raise-machine",
        "Lateral Raise (machine)",
        BodyPart.Shoulders,
        Equipment.Machine,
        Mechanic.Isolation,
    ),
)

internal val catalogExercises = repdbCatalogExercises + bybonCatalogExercises

internal fun catalogExercise(id: String): ExerciseDefinition = catalogExercisesById.getValue(id)

private val catalogExercisesById = catalogExercises.associateBy { it.id }
