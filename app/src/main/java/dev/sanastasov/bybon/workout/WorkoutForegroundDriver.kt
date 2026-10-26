package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.toOpenSnapshot
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WorkoutForegroundDriver(
    private val repository: WorkoutsRepository,
    private val scope: CoroutineScope,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    private val startService: () -> Unit,
    private val stopService: () -> Unit,
) {
    fun start() {
        scope.launch {
            var running = false
            repository.workoutSessions()
                .map { sessions ->
                    sessions.any { it.toOpenSnapshot(now()) != null }
                }
                .distinctUntilChanged()
                .collect { open ->
                    if (open && !running) {
                        startService()
                        running = true
                    } else if (!open && running) {
                        stopService()
                        running = false
                    }
                }
        }
    }
}
