package dev.sanastasov.bybon.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.sanastasov.bybon.MainActivity
import dev.sanastasov.bybon.R
import dev.sanastasov.bybon.mainModule
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.activeRestTimer
import dev.sanastasov.bybon.workout.domain.activeSession
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

private const val TAG = "WorkoutSessionService"

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        with(mainModule) {
            workoutsRepository.activeSession().mapLatest { session ->
                Log.d(TAG, "activeSession mapLatest session: $session")
                session ?: return@mapLatest
                displayNotification(session, WorkoutSessionNotificationKind.Ongoing)
                val rest = session.activeRestTimer(LocalDateTime.now()) ?: return@mapLatest
                delay(rest.remaining(LocalDateTime.now()).inWholeMilliseconds)
                displayNotification(session, WorkoutSessionNotificationKind.RestComplete)
            }.launchIn(lifecycleScope)
        }
        return START_NOT_STICKY
    }

    private fun displayNotification(
        session: WorkoutSession,
        kind: WorkoutSessionNotificationKind,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val manager = getSystemService<NotificationManager>() ?: error(
            "Failed to get NotificationManager",
        )
        ensureChannels(manager)
        val contentIntent = PendingIntent.getActivity(
            this,
            WORKOUT_SESSION_NOTIFICATION_ID,
            MainActivity.create(this, session.planId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = sessionNotification(session, contentIntent, now, kind.toPresentation())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                WORKOUT_SESSION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            ServiceCompat.startForeground(
                this,
                WORKOUT_SESSION_NOTIFICATION_ID,
                notification,
                0,
            )
        }
    }

    private fun ensureChannels(manager: NotificationManager) {
        val workoutChannel = NotificationChannel(
            WORKOUT_SESSION_CHANNEL_ID,
            getString(R.string.workout_session_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val restChannel = NotificationChannel(
            REST_COMPLETE_CHANNEL_ID,
            getString(R.string.workout_rest_complete_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(workoutChannel)
        manager.createNotificationChannel(restChannel)
        manager.deleteNotificationChannel(LEGACY_WORKOUT_SESSION_CHANNEL_ID)
    }

    private fun sessionNotification(
        session: WorkoutSession,
        contentIntent: PendingIntent,
        now: LocalDateTime,
        presentation: WorkoutSessionNotificationPresentation,
    ): Notification {
        val content = session.toNotificationContent(now)
        val title = if (presentation.restOverTitle) {
            getString(R.string.workout_session_notification_rest_over_title)
        } else {
            getString(R.string.workout_session_notification_title, content.planName)
        }
        val builder = NotificationCompat.Builder(this, presentation.channelId)
            .setSmallIcon(R.drawable.ic_workout_notification)
            .setContentTitle(title)
            .setContentText(content.contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(presentation.onlyAlertOnce)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setRequestPromotedOngoing(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(content.chronometerCountDown)
            .setWhen(content.whenTime.toEpochMilli())
            .setShowWhen(true)
        if (presentation.silent) {
            builder.setSilent(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            builder.setStyle(sessionMetricStyle(content))
        }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun sessionMetricStyle(content: WorkoutSessionNotification): NotificationCompat.Style {
        val style = NotificationCompat.MetricStyle()
        content.metrics.forEach { metric ->
            style.addMetric(metric.toCompatMetric(content))
        }
        return style.setCriticalMetric(content.criticalMetricIndex)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun WorkoutSessionNotificationMetric.toCompatMetric(
        content: WorkoutSessionNotification,
    ): NotificationCompat.Metric = when (this) {
        WorkoutSessionNotificationMetric.Elapsed -> NotificationCompat.Metric(
            NotificationCompat.Metric.TimeDifference.forStopwatch(
                content.elapsedStart,
                NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
            ),
            getString(R.string.workout_session_notification_elapsed),
        )

        WorkoutSessionNotificationMetric.Rest -> NotificationCompat.Metric(
            NotificationCompat.Metric.TimeDifference.forTimer(
                checkNotNull(content.restEnd),
                NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
            ),
            getString(R.string.workout_session_notification_rest),
        )

        WorkoutSessionNotificationMetric.Set -> NotificationCompat.Metric(
            NotificationCompat.Metric.FixedText(content.currentSetValue),
            currentSetMetricLabel(content),
        )
    }

    private fun currentSetMetricLabel(content: WorkoutSessionNotification): String {
        val labelRes = if (content.currentSetIsWarmup) {
            R.string.workout_session_notification_warmup_set
        } else {
            R.string.workout_session_notification_work_set
        }
        return getString(labelRes, content.currentSetNumber, content.currentSetTotal)
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, WorkoutSessionService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutSessionService::class.java))
        }
    }
}
