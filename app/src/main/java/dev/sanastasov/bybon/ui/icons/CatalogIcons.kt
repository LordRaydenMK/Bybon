package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.MuscleGroup

val MuscleGroup.icon: ImageVector
    get() = when (this) {
        MuscleGroup.Arms -> MuscleGroupArms
        MuscleGroup.Back -> MuscleGroupBack
        MuscleGroup.Chest -> MuscleGroupChest
        MuscleGroup.Core -> MuscleGroupCore
        MuscleGroup.FullBody -> MuscleGroupFullBody
        MuscleGroup.Legs -> MuscleGroupLegs
        MuscleGroup.Shoulders -> MuscleGroupShoulders
        MuscleGroup.Other -> MuscleGroupOther
    }

val Equipment.icon: ImageVector
    get() = when (this) {
        Equipment.Barbell -> EquipmentBarbell
        Equipment.Dumbbell -> EquipmentDumbbell
        Equipment.Machine -> EquipmentMachine
        Equipment.Bodyweight -> EquipmentBodyweight
        Equipment.AssistedBodyWeight -> EquipmentAssisted
    }
