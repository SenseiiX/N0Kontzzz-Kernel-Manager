package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.CustomTunableEntity
import id.nkz.nokontzzzmanager.data.repository.CustomTunableRepository
import id.nkz.nokontzzzmanager.data.repository.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomTunableUiState(
    val entity: CustomTunableEntity,
    val actualValue: String? = null
)

@HiltViewModel
class CustomTunableViewModel @Inject constructor(
    private val repository: CustomTunableRepository
) : ViewModel() {

    // Combine DB data with real-time system read
    val tunables: StateFlow<List<CustomTunableUiState>> = repository.getAllTunables()
        .map { list ->
            list.map { entity ->
                val actual = repository.readTunable(entity.path)
                CustomTunableUiState(entity, actual)
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fileBrowserList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileBrowserList: StateFlow<List<FileItem>> = _fileBrowserList.asStateFlow()

    private val _currentBrowserPath = MutableStateFlow("/")
    val currentBrowserPath: StateFlow<String> = _currentBrowserPath.asStateFlow()

    fun addTunable(path: String, value: String, applyOnBoot: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = CustomTunableEntity(path, value, applyOnBoot)
            repository.insertTunable(entity)
            // Auto apply on save
            repository.applyTunable(path, value)
        }
    }

    fun updateTunable(oldPath: String, tunable: CustomTunableEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (oldPath != tunable.path) {
                repository.deleteTunable(CustomTunableEntity(oldPath, "", false))
            }
            repository.insertTunable(tunable)
            // Auto apply on update (toggle boot or edit value)
            repository.applyTunable(tunable.path, tunable.value)
        }
    }

    fun deleteTunable(tunable: CustomTunableEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTunable(tunable)
        }
    }

    fun applyTunable(tunable: CustomTunableEntity, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.applyTunable(tunable.path, tunable.value)) {
                launch(Dispatchers.Main) { onSuccess() }
            } else {
                launch(Dispatchers.Main) { onError() }
            }
        }
    }

    fun readCurrentValue(path: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val value = repository.readTunable(path)
            launch(Dispatchers.Main) { onResult(value) }
        }
    }

    fun loadFileList(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.listFiles(path)
            _fileBrowserList.value = list
            _currentBrowserPath.value = path
        }
    }
    
    fun navigateUp() {
        val current = _currentBrowserPath.value
        if (current == "/") return
        
        val parent = current.substringBeforeLast('/', "")
        val nextPath = if (parent.isEmpty()) "/" else parent
        loadFileList(nextPath)
    }
}
