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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.sanastasov.bybon.MainActivity
import dev.sanastasov.bybon.R
import dev.sanastasov.bybon.mainModule
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.activeSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                session?.let { displayNotification(session) }
            }.launchIn(lifecycleScope)
        }
        return START_NOT_STICKY
    }

    private fun displayNotification(session: WorkoutSession) {
        val manager = getSystemService<NotificationManager>() ?: error(
            "Failed to get NotificationManager",
        )
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.workout_session_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
        val contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            MainActivity.create(this, session.planId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_notification)
            .setContentTitle(
                getString(R.string.workout_session_notification_title, session.planName),
            )
            .setContentText(getString(R.string.workout_session_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                0,
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "workout_session"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startService(Intent(context, WorkoutSessionService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutSessionService::class.java))
        }
    }
}
