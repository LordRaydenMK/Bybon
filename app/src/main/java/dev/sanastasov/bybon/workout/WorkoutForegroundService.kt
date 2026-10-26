package dev.sanastasov.bybon.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.sanastasov.bybon.BybonApplication
import dev.sanastasov.bybon.R
import dev.sanastasov.bybon.workout.domain.toOpenSnapshot
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkoutForegroundService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        bybon.workoutModule.attachSessionRuntime(lifecycleScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startPlaceholderForeground()
        lifecycleScope.launch {
            val sessions = bybon.workoutModule.workoutsRepository.workoutSessions().first()
            val open = sessions.any { it.toOpenSnapshot(LocalDateTime.now()) != null }
            if (!open) {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        bybon.workoutModule.detachSessionRuntime()
        super.onDestroy()
    }

    private fun startPlaceholderForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.workout_session_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_notification)
            .setContentTitle(getString(R.string.workout_session_notification_title))
            .setContentText(getString(R.string.workout_session_notification_text))
            .setOngoing(true)
            .setSilent(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    private val bybon: BybonApplication
        get() = getApplication() as BybonApplication

    private companion object {
        const val CHANNEL_ID = "workout_session"
        const val NOTIFICATION_ID = 1001
    }
}
