package dev.sanastasov.bybon.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

internal class WorkoutSessionRestAlarm(
    private val context: Context,
    private val alarmManager: AlarmManager,
) : RestPromoteScheduler {
    private val continuation = AtomicReference<CancellableContinuation<Unit>?>(null)

    fun completeFrom(intent: Intent?) {
        if (intent?.action != ACTION) return
        continuation.getAndSet(null)?.takeIf { it.isActive }?.resume(Unit)
    }

    override fun schedule(wait: Duration) {
        val waitMs = wait.inWholeMilliseconds.coerceAtLeast(0L)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + waitMs,
            pendingIntent(),
        )
    }

    suspend fun await(wait: Duration) {
        val waitMs = wait.inWholeMilliseconds
        if (waitMs <= 0L) return
        val targetElapsedRealtime = SystemClock.elapsedRealtime() + waitMs
        if (!alarmManager.canScheduleExactAlarms()) {
            delay(waitMs)
            return
        }
        try {
            suspendCancellableCoroutine { cont ->
                continuation.set(cont)
                cont.invokeOnCancellation {
                    continuation.compareAndSet(cont, null)
                    cancel()
                }
                schedule(wait)
            }
        } catch (_: SecurityException) {
            delay((targetElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
        } finally {
            continuation.set(null)
            cancel()
        }
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, WorkoutSessionService::class.java).setAction(ACTION)
        return PendingIntent.getForegroundService(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val ACTION = "dev.sanastasov.bybon.action.REST_ALARM"
        private const val REQUEST_CODE = 1002
    }
}
