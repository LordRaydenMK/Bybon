package dev.sanastasov.bybon.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.getSystemService
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

internal class WorkoutSessionRestAlarm(
    private val context: Context,
) {
    private val continuation = AtomicReference<CancellableContinuation<Unit>?>(null)

    fun completeFrom(intent: Intent?) {
        if (intent?.action != ACTION) return
        continuation.getAndSet(null)?.takeIf { it.isActive }?.resume(Unit)
    }

    suspend fun await(wait: Duration) {
        val waitMs = wait.inWholeMilliseconds
        if (waitMs <= 0L) return
        val targetElapsedRealtime = SystemClock.elapsedRealtime() + waitMs
        if (!canScheduleExact()) {
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
                alarmManager().setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    targetElapsedRealtime,
                    pendingIntent(),
                )
            }
        } catch (_: SecurityException) {
            delay((targetElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
        } finally {
            continuation.set(null)
            cancel()
        }
    }

    fun cancel() {
        context.getSystemService<AlarmManager>()?.cancel(pendingIntent())
    }

    private fun canScheduleExact(): Boolean = alarmManager().canScheduleExactAlarms()

    private fun alarmManager(): AlarmManager = context.getSystemService()
        ?: error("Failed to get AlarmManager")

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
