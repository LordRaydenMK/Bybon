package dev.sanastasov.bybon.workout.domain

internal val bybonCatalogExercises = listOf(
    ExerciseDefinition(
        id = "iso-lat-row",
        name = "Iso-Lateral Row (machine)",
        bodyPart = BodyPart.Back,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Compound,
        primaryMuscles = listOf("latissimus_dorsi", "rhomboids"),
        secondaryMuscles = listOf("biceps_brachii", "posterior_deltoid"),
    ),
    ExerciseDefinition(
        id = "squat-machine",
        name = "Squat (machine)",
        bodyPart = BodyPart.UpperLegs,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Compound,
        primaryMuscles = listOf("gluteus_maximus", "quadriceps"),
        secondaryMuscles = listOf("erector_spinae", "hamstrings"),
    ),
    ExerciseDefinition(
        id = "triceps-press-machine",
        name = "Triceps Press (machine)",
        bodyPart = BodyPart.UpperArms,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Isolation,
        primaryMuscles = listOf("triceps_brachii"),
    ),
    ExerciseDefinition(
        id = "lateral-raise-machine",
        name = "Lateral Raise (machine)",
        bodyPart = BodyPart.Shoulders,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Isolation,
        primaryMuscles = listOf("lateral_deltoid"),
        secondaryMuscles = listOf("anterior_deltoid", "trapezius"),
    ),
)

internal val catalogExercises = repdbCatalogExercises + bybonCatalogExercises

internal fun catalogExercise(id: String): ExerciseDefinition = catalogExercisesById.getValue(id)

private val catalogExercisesById = catalogExercises.associateBy { it.id }
