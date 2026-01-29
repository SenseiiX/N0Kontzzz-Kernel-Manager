package id.nkz.nokontzzzmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.repository.CustomTunableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomTunableRestoreService : Service() {

    @Inject
    lateinit var repository: CustomTunableRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "CustomTunableRestore"
    private val CHANNEL_ID = "custom_tunable_restore_channel"
    private val NOTIFICATION_ID = 1002

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started for aggressive restore")
        
        // Start as Foreground to prevent system kill during boot
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            monitorAndEnforceTunables()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun monitorAndEnforceTunables() {
        val tunables = repository.getBootTunables()
        if (tunables.isEmpty()) return

        Log.d(TAG, "Found ${tunables.size} tunables to restore. Starting 60s enforcement loop.")

        // Duration to monitor: 60 seconds
        // Interval: 3 seconds
        val maxRetries = 20 // 20 * 3s = 60s
        
        for (i in 0 until maxRetries) {
            var allCorrect = true
            
            tunables.forEach { tunable ->
                try {
                    // Check current value
                    val current = repository.readTunable(tunable.path)
                    
                    if (current != tunable.value) {
                        Log.w(TAG, "Mismatch detected on loop $i: ${tunable.path}. Got '$current', enforcing '${tunable.value}'")
                        
                        // Force Apply
                        repository.applyTunable(tunable.path, tunable.value)
                        allCorrect = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking ${tunable.path}", e)
                }
            }

            // Optional: If everything is correct for the last 5 checks, maybe we can exit early?
            // For safety against late init scripts, we stick to the full duration mostly, 
            // or maybe exit if stable for 30s. But simpler is to just run the full course.
            
            delay(3000)
        }
        
        Log.d(TAG, "Enforcement loop finished.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Boot Restore Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Applying custom kernel settings on boot"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NKM: Applying Settings")
            .setContentText("Enforcing custom tunables...")
            .setSmallIcon(R.drawable.ic_speed) // Ensure this drawable exists or use fallback
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}