package dev.sanastasov.bybon.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlin.time.Duration

internal const val REST_PROMOTE_ALARM_ACTION = "dev.sanastasov.bybon.action.REST_ALARM"
internal const val COMPLETE_SET_ACTION = "dev.sanastasov.bybon.action.COMPLETE_SET"
private const val REST_PROMOTE_ALARM_REQUEST_CODE = 1002
private const val COMPLETE_SET_REQUEST_CODE = 1003

internal fun completeSetPendingIntent(context: Context): PendingIntent {
    val appContext = context.applicationContext
    val intent = Intent(appContext, WorkoutSessionService::class.java)
        .setAction(COMPLETE_SET_ACTION)
    return PendingIntent.getService(
        appContext,
        COMPLETE_SET_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

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
