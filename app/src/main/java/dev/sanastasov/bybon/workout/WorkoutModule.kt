package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.workout.data.WorkoutsRepositoryImpl
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface WorkoutModule : DbModule {

    val workoutsRepository: WorkoutsRepository

    val sessionRuntime: StateFlow<WorkoutSessionRuntime?>

    fun attachSessionRuntime(
        scope: CoroutineScope,
        now: () -> LocalDateTime = LocalDateTime::now,
    ): WorkoutSessionRuntime

    fun detachSessionRuntime()

    companion object {

        private var repository: WorkoutsRepository? = null

        fun create(dbModule: DbModule): WorkoutModule {
            val db = dbModule.bybonDb
            val workouts = repository ?: WorkoutsRepositoryImpl(
                exerciseDao = db.exerciseDao(),
                workoutPlanDao = db.workoutPlanDao(),
                workoutSessionDao = db.workoutSessionDao(),
            ).also { repository = it }
            val runtime = MutableStateFlow<WorkoutSessionRuntime?>(null)
            return object : WorkoutModule, DbModule by dbModule {
                override val workoutsRepository: WorkoutsRepository = workouts

                override val sessionRuntime: StateFlow<WorkoutSessionRuntime?> =
                    runtime.asStateFlow()

                override fun attachSessionRuntime(
                    scope: CoroutineScope,
                    now: () -> LocalDateTime,
                ): WorkoutSessionRuntime {
                    runtime.value?.let { return it }
                    return WorkoutSessionRuntimeImpl(workouts, scope, now)
                        .also { runtime.value = it }
                }

                override fun detachSessionRuntime() {
                    runtime.value = null
                }
            }
        }
    }
}
