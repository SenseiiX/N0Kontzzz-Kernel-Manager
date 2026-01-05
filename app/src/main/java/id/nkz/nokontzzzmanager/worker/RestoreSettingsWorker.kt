package id.nkz.nokontzzzmanager.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager

@HiltWorker
class RestoreSettingsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val systemRepository: SystemRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RestoreSettingsWorker", "Restoring system settings...")

        // Restore TCP Congestion Algorithm
        val savedTcpAlgo = preferenceManager.getTcpCongestionAlgorithm()
        if (!savedTcpAlgo.isNullOrEmpty()) {
            val success = systemRepository.setTcpCongestionAlgorithm(savedTcpAlgo)
            Log.d("RestoreSettingsWorker", "Restored TCP Congestion to $savedTcpAlgo: $success")
        }

        // Restore I/O Scheduler
        val savedIoScheduler = preferenceManager.getIoScheduler()
        if (!savedIoScheduler.isNullOrEmpty()) {
            val success = systemRepository.setIoScheduler(savedIoScheduler)
            Log.d("RestoreSettingsWorker", "Restored I/O Scheduler to $savedIoScheduler: $success")
        }

        // Future: Restore other settings here (e.g. IO Scheduler, etc)

        return Result.success()
    }
}
