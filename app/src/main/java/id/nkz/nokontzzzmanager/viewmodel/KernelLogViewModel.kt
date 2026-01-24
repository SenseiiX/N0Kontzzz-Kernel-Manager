package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import id.nkz.nokontzzzmanager.util.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class KernelLogViewModel @Inject constructor(
    private val rootRepository: RootRepository,
    private val themeManager: ThemeManager
) : ViewModel() {

    val isAmoledMode = themeManager.isAmoledMode

    private val _logContent = MutableStateFlow<List<String>>(emptyList())
    val logContent: StateFlow<List<String>> = _logContent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var monitoringJob: kotlinx.coroutines.Job? = null
    private var isFirstLoad = true

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        
        monitoringJob = viewModelScope.launch {
            while (true) {
                loadLogsInternal(quiet = !isFirstLoad)
                isFirstLoad = false
                kotlinx.coroutines.delay(2000) // Poll every 2 seconds
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun loadLogs() {
        viewModelScope.launch {
            loadLogsInternal(quiet = false)
        }
    }

    private suspend fun loadLogsInternal(quiet: Boolean) {
        if (!quiet) {
            _isLoading.value = true
        }
        _error.value = null
        try {
            // Execute dmesg in background
            val result = withContext(Dispatchers.IO) {
                try {
                     rootRepository.run("dmesg")
                } catch (e: Exception) {
                    throw e
                }
            }
            
            if (result.isNotBlank()) {
                val lines = result.lines()
                // Only update if content has changed to avoid unnecessary recompositions
                if (lines != _logContent.value) {
                    _logContent.value = lines
                }
            } else {
                _logContent.value = emptyList()
            }
        } catch (e: Exception) {
            if (!quiet) {
                _error.value = e.message
                _logContent.value = emptyList()
            }
        } finally {
            if (!quiet) {
                _isLoading.value = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    rootRepository.run("dmesg -C")
                }
                loadLogsInternal(quiet = false)
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
