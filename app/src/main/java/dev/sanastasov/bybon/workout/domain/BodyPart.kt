package dev.sanastasov.bybon.workout.domain

enum class BodyPart {
    UpperLegs,
    Back,
    UpperArms,
    Chest,
    Shoulders,
    Core,
    LowerLegs,
    LowerArms,
    FullBody,
}

val BodyPart.label: String
    get() = when (this) {
        BodyPart.UpperLegs -> "Upper legs"
        BodyPart.LowerLegs -> "Lower legs"
        BodyPart.UpperArms -> "Upper arms"
        BodyPart.LowerArms -> "Lower arms"
        BodyPart.FullBody -> "Full body"
        else -> name
    }
