package dev.sanastasov.bybon.workout.domain

val exercises = listOf(
    // Arms
    Exercise("incline-curl-db", "Incline Curl (dumbbell)", MuscleGroup.Arms, Equipment.Dumbbell),
    Exercise("biceps-curl-machine", "Curl (machine)", MuscleGroup.Arms, Equipment.Machine),
    Exercise("skullcrusher-db", "Skullcrusher (dumbbell)", MuscleGroup.Arms, Equipment.Dumbbell),
    Exercise(
        "triceps-press-machine",
        "Triceps Press (machine)",
        MuscleGroup.Arms,
        Equipment.Machine
    ),
    // Back
    Exercise("incline-row-db", "Incline Row (dumbbell)", MuscleGroup.Back, Equipment.Dumbbell),
    Exercise(
        "pullup-assisted",
        "Pull Up (assisted)",
        MuscleGroup.Back,
        Equipment.AssistedBodyWeight
    ),
    Exercise("lat-pull-down", "Lat Pull-down (cable)", MuscleGroup.Back, Equipment.Machine),
    Exercise("iso-lat-row", "Iso-Lateral Row (machine)", MuscleGroup.Back, Equipment.Machine),
    Exercise("deadlift-barbell", "Deadlift (barbell)", MuscleGroup.Back, Equipment.Barbell),
    // Chest
    Exercise("bench-press-bb", "Bench Press (barbell)", MuscleGroup.Chest, Equipment.Barbell),
    Exercise(
        "incline-bench-press-bb",
        "Incline Bench Press (barbell)",
        MuscleGroup.Chest,
        Equipment.Barbell
    ),
    Exercise(
        "incline-bench-press-db",
        "Incline Bench Press (dumbbell)",
        MuscleGroup.Chest,
        Equipment.Dumbbell
    ),
    Exercise("bench-press-db", "Bench Press (dumbbell)", MuscleGroup.Chest, Equipment.Dumbbell),
    Exercise("chest-press-machine", "Chest Press (machine)", MuscleGroup.Chest, Equipment.Machine),
    Exercise("chest-fly-cable", "Chest Fly (cable)", MuscleGroup.Chest, Equipment.Machine),
    Exercise("chest-fly-peck-deck", "Chest Fly (machine)", MuscleGroup.Chest, Equipment.Machine),
    Exercise("chest-dip", "Chest Dip (assisted)", MuscleGroup.Chest, Equipment.AssistedBodyWeight),
    // Legs
    Exercise("squat-bb", "Squat (barbell)", MuscleGroup.Legs, Equipment.Barbell),
    Exercise("rdl-bb", "Romanian Deadlift (RDL) (barbell)", MuscleGroup.Legs, Equipment.Barbell),
    Exercise(
        "split-squat-db",
        "Bulgarian Split Squat (dumbbell)",
        MuscleGroup.Legs,
        Equipment.Dumbbell
    ),
    Exercise("leg-curl", "Leg Curl (machine)", MuscleGroup.Legs, Equipment.Machine),
    Exercise("leg-press", "Leg Press (machine)", MuscleGroup.Legs, Equipment.Machine),
    Exercise("leg-extension", "Leg Extension (machine)", MuscleGroup.Legs, Equipment.Machine),
    Exercise("squat-machine", "Squat (machine)", MuscleGroup.Legs, Equipment.Machine),
    Exercise(
        "standing-calf-raise-machine",
        "Standing Calf Raise (machine)",
        MuscleGroup.Legs,
        Equipment.Machine
    ),
    // Shoulders
    Exercise(
        "lateral-raise-db",
        "Lateral Raise (dumbbell)",
        MuscleGroup.Shoulders,
        Equipment.Dumbbell
    ),
    Exercise(
        "lateral-raise-cable",
        "Lateral Raise (cable)",
        MuscleGroup.Shoulders,
        Equipment.Machine
    ),
    Exercise("upright-row-db", "Upright Row (dumbbell)", MuscleGroup.Shoulders, Equipment.Dumbbell),
    Exercise(
        "lateral-raise-machine",
        "Lateral Raise (machine)",
        MuscleGroup.Shoulders,
        Equipment.Dumbbell
    ),
    Exercise("face-pull", "Face Pull (cable)", MuscleGroup.Shoulders, Equipment.Machine),
    Exercise(
        "overhead-press-bb",
        "Overhead Press (barbell)",
        MuscleGroup.Shoulders,
        Equipment.Barbell
    ),
)

val exercisesMap = exercises.associateBy { it.id }

data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscleGroup: MuscleGroup,
    val equipment: Equipment
)
