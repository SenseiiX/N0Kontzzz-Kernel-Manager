package id.nkz.nokontzzzmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.viewmodel.KernelLogViewModel
import androidx.compose.ui.res.stringResource
import id.nkz.nokontzzzmanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KernelLogScreen(
    navController: NavController,
    viewModel: KernelLogViewModel = hiltViewModel(),
    onSetActions: (@Composable RowScope.() -> Unit) -> Unit
) {
    val logContent by viewModel.logContent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        viewModel.startMonitoring()
        onSetActions {
            IconButton(onClick = { viewModel.loadLogs() }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.kernel_log_refresh))
            }
            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.kernel_log_clear))
            }
        }
        onDispose {
            viewModel.stopMonitoring()
            onSetActions {}
        }
    }
    
    // Auto-scroll to bottom when logs are initially loaded or updated if user is already at bottom
    LaunchedEffect(logContent.size) {
        if (logContent.isNotEmpty()) {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            if (totalItems == 0 || lastVisibleItemIndex >= totalItems - 10) {
                 listState.animateScrollToItem(logContent.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && logContent.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
                Column(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.kernel_log_error_fetch, error ?: "Unknown"), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.loadLogs() }) {
                    Text(stringResource(R.string.kernel_log_retry))
                }
            }
        } else {
            if (logContent.isEmpty()) {
                    Text(
                    text = stringResource(R.string.kernel_log_empty),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(logContent) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
        
        // Show loading indicator overlay if refreshing but content exists
        if (isLoading && logContent.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}
