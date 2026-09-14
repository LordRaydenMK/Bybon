package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDate

@Entity("diet_phase")
data class DietPhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val startDate: LocalDate,
    val startWeight: Int,
    val targetWeight: Int,
    val durationWeeks: Int?,
    val endedAt: LocalDate?,
)
