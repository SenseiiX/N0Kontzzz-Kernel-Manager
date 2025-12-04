package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryHistoryViewModel @Inject constructor(
    private val repository: BatteryGraphRepository
) : ViewModel() {

    // Get all history
    val historyData: StateFlow<List<BatteryGraphEntry>> = repository
        .getAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAllEntries()
        }
    }
}