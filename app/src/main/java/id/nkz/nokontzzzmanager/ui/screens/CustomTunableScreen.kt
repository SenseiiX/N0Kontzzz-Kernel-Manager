package id.nkz.nokontzzzmanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.CustomTunableEntity
import id.nkz.nokontzzzmanager.ui.MainActivity
import id.nkz.nokontzzzmanager.viewmodel.CustomTunableUiState
import id.nkz.nokontzzzmanager.viewmodel.CustomTunableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTunableScreen(
    navController: NavController,
    viewModel: CustomTunableViewModel = hiltViewModel()
) {
    val tunables by viewModel.tunables.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mainActivity = remember(context) { context as? MainActivity }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTunable by remember { mutableStateOf<CustomTunableEntity?>(null) }

    DisposableEffect(Unit) {
        mainActivity?.customTunableFabVisible?.value = true
        mainActivity?.customTunableFabAction?.value = { showAddDialog = true }

        onDispose {
            mainActivity?.customTunableFabVisible?.value = false
            mainActivity?.customTunableFabAction?.value = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tunables, key = { it.entity.path }) { state ->
                CustomTunableCard(
                    uiState = state,
                    onEdit = {
                        editingTunable = state.entity
                        showAddDialog = true
                    },
                    onDelete = {
                        viewModel.deleteTunable(state.entity)
                    },
                    onToggleBoot = { enabled ->
                        viewModel.updateTunable(state.entity.copy(applyOnBoot = enabled))
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }

    if (showAddDialog) {
        CustomTunableDialog(
            viewModel = viewModel,
            initialTunable = editingTunable,
            onDismiss = {
                showAddDialog = false
                editingTunable = null
            },
            onSave = { path, value, applyOnBoot ->
                if (editingTunable != null) {
                     viewModel.updateTunable(CustomTunableEntity(path, value, applyOnBoot))
                } else {
                     viewModel.addTunable(path, value, applyOnBoot)
                }
                showAddDialog = false
                editingTunable = null
            },
            onReadValue = { path, callback ->
                viewModel.readCurrentValue(path, callback)
            }
        )
    }
}

@Composable
fun CustomTunableCard(
    uiState: CustomTunableUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleBoot: (Boolean) -> Unit
) {
    val tunable = uiState.entity
    val actualValue = uiState.actualValue
    val isMismatch = actualValue != null && actualValue != tunable.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tunable.path,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target: ${tunable.value}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (actualValue != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Actual: $actualValue",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontWeight = if (isMismatch) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = tunable.applyOnBoot,
                        onCheckedChange = onToggleBoot,
                        modifier = Modifier.scale(0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.set_on_boot),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// Extension to scale Switch
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

@Composable
fun CustomTunableDialog(
    viewModel: CustomTunableViewModel,
    initialTunable: CustomTunableEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit,
    onReadValue: (String, (String) -> Unit) -> Unit
) {
    var path by remember { mutableStateOf(initialTunable?.path ?: "") }
    var value by remember { mutableStateOf(initialTunable?.value ?: "") }
    var applyOnBoot by remember { mutableStateOf(initialTunable?.applyOnBoot ?: false) }
    
    var showFilePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    if (showFilePicker) {
        val fileList by viewModel.fileBrowserList.collectAsStateWithLifecycle()
        val currentBrowserPath by viewModel.currentBrowserPath.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            val initialPath = if (path.startsWith("/")) path.substringBeforeLast('/', "") else "/"
            val startPath = if (initialPath.isEmpty()) "/" else initialPath
            viewModel.loadFileList(startPath)
        }

        RootFilePickerDialog(
            currentPath = currentBrowserPath,
            fileList = fileList,
            onNavigate = { newPath -> viewModel.loadFileList(newPath) },
            onNavigateUp = { viewModel.navigateUp() },
            onFileSelected = { selectedPath ->
                path = selectedPath
                showFilePicker = false
                onReadValue(selectedPath) { readVal -> 
                    value = readVal
                    Toast.makeText(context, "Value Read", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showFilePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTunable == null) stringResource(R.string.add_tunable) else stringResource(R.string.edit_tunable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Path (/proc/..., /sys/...)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { 
                            showFilePicker = true
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Pick File")
                        }
                    }
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (path.isNotEmpty()) {
                                onReadValue(path) { readVal ->
                                    value = readVal
                                    Toast.makeText(context, "Value Read", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Read Current")
                        }
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = applyOnBoot,
                        onCheckedChange = { applyOnBoot = it }
                    )
                    Text(stringResource(R.string.set_on_boot))
                }
                
                Text(
                    text = "Tip: Use the folder icon to browse root directories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (path.isNotBlank()) {
                        onSave(path, value, applyOnBoot)
                    } else {
                        Toast.makeText(context, "Path is required", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(stringResource(R.string.app_profiles_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}