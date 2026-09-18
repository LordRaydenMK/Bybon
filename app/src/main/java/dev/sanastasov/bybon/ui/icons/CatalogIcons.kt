package dev.sanastasov.bybon.ui.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@Preview
@Composable
private fun CatalogIconsPreview() {
    Surface {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Muscle groups")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MuscleGroup.entries.forEach { muscleGroup ->
                    Icon(muscleGroup.icon, muscleGroup.name, Modifier.size(24.dp))
                }
            }
            Text("Equipment")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Equipment.entries.forEach { equipment ->
                    Icon(equipment.icon, equipment.name, Modifier.size(24.dp))
                }
            }
        }
    }
}
