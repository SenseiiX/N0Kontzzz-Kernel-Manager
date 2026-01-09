package id.nkz.nokontzzzmanager

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import android.util.Log

import id.nkz.nokontzzzmanager.utils.LocaleHelper
import android.content.Context
import javax.inject.Inject
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import id.nkz.nokontzzzmanager.service.BatteryMonitorService

@HiltAndroidApp
class NkzApp : Application(), Configuration.Provider {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()

        // Initialize Superuser shell with proper flags
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )

        // Initialize theme mode - this will be managed by ThemeManager
        // The actual theme will be applied in the MainActivity based on user preference

        // Auto-start Battery Monitor if enabled when app process starts
        runCatching {
            if (preferenceManager.isBatteryMonitorEnabled()) {
                BatteryMonitorService.start(this)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG) // Logging untuk debugging
            .build()
}