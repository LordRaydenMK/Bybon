package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class WorkoutPlanMappingTest {

    @Test
    fun `round trip keeps header rest seconds notes and live exercise rows`() {
        val entity = fullBodyA.toEntity()
        val lines = fullBodyA.toExerciseEntities()
        val exercises = fullBodyA.sets.map {
            it.exercise.toEntity(source = ExerciseSource.SHIPPED)
        }

        val restored = listOf(entity).toDomainPlans(lines, exercises).single()

        assert(restored == fullBodyA)
        assert(entity.id == "full-body-a")
        assert(entity.isArchived == false)
        assert(lines.first().position == 0)
        assert(lines.first().exerciseId == "bench-press")
        assert(lines.first().warmupSets == 3)
        assert(lines.first().workSets == 3)
        assert(lines.first().repMin == 8)
        assert(lines.first().repMax == 10)
        val restSeconds = fullBodyA.sets.first().restAfterWorkSet.inWholeSeconds.toInt()
        assert(lines.first().restAfterWorkSetSeconds == restSeconds)
        assert(lines.first().notes == "[]")
        assert(fullBodyA.sets.first().restAfterWorkSet == 120.seconds)
    }

    @Test
    fun `incomplete plan snapshots are omitted until children exist`() {
        val entity = fullBodyA.toEntity()
        val exercises = fullBodyA.sets.map {
            it.exercise.toEntity(source = ExerciseSource.SHIPPED)
        }

        assert(listOf(entity).toDomainPlans(lines = emptyList(), exercises = exercises).isEmpty())
        assert(
            listOf(entity).toDomainPlans(fullBodyA.toExerciseEntities(), exercises).single() ==
                fullBodyA,
        )
    }
}
