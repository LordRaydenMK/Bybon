package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class PlannedExerciseUiTest {

    @Test
    fun `bench press expands warmup and work rows`() {
        val bench = fullBodyA.sets.first()
        val rows = bench.toPlannedSets()

        assert(rows.size == 6)
        assert(rows.take(3).all { it.isWarmup && it.repsLabel == "—" && it.rest == null })
        assert(rows.drop(3).map { it.workSetNumber } == listOf(1, 2, 3))
        assert(rows.drop(3).all { it.repsLabel == "8–10" && it.rest == 2.minutes })
        assert(bench.subtitle() == "Chest · Barbell")
        assert(rows.first().contentDescription(bench.exercise.name) == "Bench Press (barbell) warmup set")
        assert(
            rows[3].contentDescription(bench.exercise.name) ==
                "Bench Press (barbell) set 1, 8–10 reps, rest 2:00",
        )
    }

    @Test
    fun `leg curl has no warmup rows`() {
        val curl = fullBodyA.sets.first { it.exercise.id == "leg-curl" }
        val rows = curl.toPlannedSets()

        assert(rows.size == 3)
        assert(rows.none { it.isWarmup })
        assert(rows.all { it.repsLabel == "12–14" && it.rest == 90.seconds })
        assert(curl.subtitle() == "Legs · Machine")
        assert(
            rows.first().contentDescription(curl.exercise.name) ==
                "Leg Curl (machine) set 1, 12–14 reps, rest 1:30",
        )
    }

    @Test
    fun `assisted equipment uses a short label`() {
        val pullUp = fullBodyA.sets.first { it.exercise.id == "pullup-assisted" }
        assert(pullUp.subtitle() == "Back · Assisted")
    }
}
