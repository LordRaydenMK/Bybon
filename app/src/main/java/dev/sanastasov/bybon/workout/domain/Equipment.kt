package dev.sanastasov.bybon.workout.domain

enum class Equipment {
    Barbell,
    Dumbbell,
    Machine,
    Bodyweight,
    AssistedBodyWeight,
}

val Equipment.weightIncrement: Weight?
    get() = when (this) {
        Equipment.Barbell -> Weight.kilograms(2.5f)
        Equipment.Dumbbell -> Weight.kilograms(2f)
        Equipment.Machine, Equipment.AssistedBodyWeight -> Weight.kilograms(2.5f)
        Equipment.Bodyweight -> null
    }

val Equipment.defaultWarmupWeight: Weight
    get() = when (this) {
        Equipment.Dumbbell -> Weight.kilograms(10)

        Equipment.Barbell,
        Equipment.Machine,
        Equipment.AssistedBodyWeight,
        Equipment.Bodyweight,
        -> Weight.kilograms(20)
    }

val Equipment.label: String
    get() = when (this) {
        Equipment.AssistedBodyWeight -> "Assisted"
        else -> name
    }
