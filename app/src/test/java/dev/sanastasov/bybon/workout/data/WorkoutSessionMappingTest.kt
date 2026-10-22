package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class WorkoutSessionMappingTest {

    @Test
    fun `round trip keeps header sets previous performance and warmup rows`() {
        val startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34)
        val original = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val write = original.toWrite(sessionId = 7)

        val lines = write.exercises.mapIndexed { index, exercise ->
            exercise.entity.copy(id = index + 1L, sessionId = 7)
        }
        val sets = write.exercises.flatMapIndexed { index, exercise ->
            exercise.sets.map { it.copy(sessionExerciseId = index + 1L) }
        }
        val catalog = original.exercises.map {
            it.exerciseDefinition.toEntity(source = ExerciseSource.SHIPPED)
        }

        val restored = listOf(write.header.copy(id = 7))
            .toDomainSessions(lines, sets, catalog)
            .single()

        assert(restored == original)
        assert(write.header.planId == "full-body-a")
        assert(write.header.startedAt == startedAt)
        assert(write.header.durationSeconds == null)
        assert(write.header.planName == fullBodyA.name)
        assert(write.header.note == fullBodyA.description)
        val firstSets = write.exercises.first().sets
        assert(firstSets.take(3).all { it.isWarmup })
        assert(firstSets.drop(3).none { it.isWarmup })
        assert(firstSets.first().setState == SetState.InProgress.name)
        assert(Weight.kilograms(32.5f).toHundredths() == 3250)
    }

    @Test
    fun `completed session stores duration seconds and reconstructs null warmup`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34),
            exercises = listOf(completedExercise("bench-press", 80f to 8, 82.5f to 6)),
        )
        val write = session.toWrite(sessionId = 1)
        val line = write.exercises.single().entity.copy(id = 1, sessionId = 1)
        val sets = write.exercises.single().sets.map { it.copy(sessionExerciseId = 1) }
        val catalog = listOf(
            session.exercises.single().exerciseDefinition.toEntity(source = ExerciseSource.SHIPPED),
        )

        val restored = listOf(write.header.copy(id = 1))
            .toDomainSessions(listOf(line), sets, catalog)
            .single()

        assert(restored == session)
        assert(write.header.durationSeconds == 40.minutes.inWholeSeconds.toInt())
        assert(sets.none { it.isWarmup })
        assert(sets.all { it.setState == SetState.Completed.name })
        assert(sets.first().weight == 8000)
        assert(sets.last().weight == 8250)
    }

    @Test
    fun `incomplete session snapshots are omitted until children exist`() {
        val header = WorkoutSessionEntity(
            id = 1,
            planId = "full-body-a",
            planName = "Full Body A",
            note = null,
            startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34),
            durationSeconds = null,
        )

        assert(
            listOf(header).toDomainSessions(
                lines = emptyList(),
                sets = emptyList(),
                exercises = emptyList(),
            ).isEmpty(),
        )
    }
}
