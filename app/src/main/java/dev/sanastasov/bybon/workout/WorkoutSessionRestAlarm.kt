package dev.sanastasov.bybon.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlin.time.Duration

internal const val REST_PROMOTE_ALARM_ACTION = "dev.sanastasov.bybon.action.REST_ALARM"
private const val REST_PROMOTE_ALARM_REQUEST_CODE = 1002

internal fun restPromotePendingIntent(context: Context): PendingIntent {
    val appContext = context.applicationContext
    val intent = Intent(appContext, WorkoutSessionService::class.java)
        .setAction(REST_PROMOTE_ALARM_ACTION)
    return PendingIntent.getForegroundService(
        appContext,
        REST_PROMOTE_ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

internal fun AlarmManager.setExactAfter(wait: Duration, operation: PendingIntent) {
    val waitMs = wait.inWholeMilliseconds
    check(waitMs > 0L) { "Exact alarm wait must be positive" }
    setExactAndAllowWhileIdle(
        AlarmManager.ELAPSED_REALTIME_WAKEUP,
        SystemClock.elapsedRealtime() + waitMs,
        operation,
    )
}

internal class WorkoutSessionRestAlarm(
    private val alarmManager: AlarmManager,
    private val pendingIntent: PendingIntent,
) : RestPromoteScheduler {
    override fun schedule(wait: Duration) {
        alarmManager.setExactAfter(wait, pendingIntent)
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent)
    }
}
