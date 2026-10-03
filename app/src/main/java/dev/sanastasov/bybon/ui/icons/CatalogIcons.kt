package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Equipment

val BodyPart.icon: ImageVector
    get() = when (this) {
        BodyPart.UpperArms, BodyPart.LowerArms -> MuscleGroupArms
        BodyPart.Back -> MuscleGroupBack
        BodyPart.Chest -> MuscleGroupChest
        BodyPart.Core -> MuscleGroupCore
        BodyPart.FullBody -> MuscleGroupFullBody
        BodyPart.UpperLegs, BodyPart.LowerLegs -> MuscleGroupLegs
        BodyPart.Shoulders -> MuscleGroupShoulders
    }

val Equipment.icon: ImageVector
    get() = when (this) {
        Equipment.Barbell -> EquipmentBarbell
        Equipment.Dumbbell -> EquipmentDumbbell
        Equipment.Machine -> EquipmentMachine
        Equipment.Bodyweight -> EquipmentBodyweight
        Equipment.AssistedBodyWeight -> EquipmentAssisted
    }
