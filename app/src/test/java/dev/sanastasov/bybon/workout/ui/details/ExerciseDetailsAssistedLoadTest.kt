package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.workout.data.completedAssistedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import java.time.LocalDateTime
import org.junit.Test

class ExerciseDetailsAssistedLoadTest {

    @Test
    fun `history and PR use zero kilograms when assisted load is missing`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 10, 1, 18, 0),
            exercises = listOf(
                completedAssistedExercise(
                    "assisted-pull-ups",
                    80f,
                    25f to 8,
                    null to 6,
                ),
            ),
        )
        val now = LocalDateTime.of(2026, 10, 22, 18, 0)
        val ui = catalogExercise("assisted-pull-ups").toDetailsUi(
            listOf(session).exercisePerformance("assisted-pull-ups", now),
            now,
        )

        val unassistedOneRm = Weight.kilograms(estimateOneRmKg(80f, 6))
        val personalRecord = checkNotNull(ui.personalRecord)
        assert(personalRecord.weightKg == "0")
        assert(personalRecord.reps == 6)
        assert(personalRecord.oneRm == unassistedOneRm)
        assert(
            personalRecordOneRmLabel(personalRecord) ==
                "${unassistedOneRm.kilograms} kg · 0 kg x 6",
        )
        val sets = ui.recentSessions.single().sets
        assert(sets[0].weightKg == "-25")
        assert(sets[1].weightKg == "0")
        assert(historySetLoadLabel(sets[1]) == "2. 0 kg x 6")
        val point = ui.oneRmTrend.single()
        assert(point.weightKg == "0")
        assert(oneRmTrendPopupText(ui.oneRmTrend, 0).contains("0 kg x 6"))
    }
}
