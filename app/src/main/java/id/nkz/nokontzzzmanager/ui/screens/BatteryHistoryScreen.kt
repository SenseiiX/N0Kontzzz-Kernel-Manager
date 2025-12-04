package id.nkz.nokontzzzmanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.ui.MainActivity
import id.nkz.nokontzzzmanager.viewmodel.BatteryHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryHistoryScreen(
    navController: NavController,
    viewModel: BatteryHistoryViewModel = hiltViewModel()
) {
    val historyData by viewModel.historyData.collectAsState()
    var graphMode by remember { mutableStateOf(BatteryGraphMode.SPEED) }
    var showClearDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mainActivity = remember(context) { context as? MainActivity }

    LaunchedEffect(Unit) {
        mainActivity?.batteryHistoryFabVisible?.value = true
        mainActivity?.batteryHistoryFabAction?.value = { showClearDialog = true }
    }

    DisposableEffect(Unit) {
        onDispose {
            mainActivity?.batteryHistoryFabVisible?.value = false
            mainActivity?.batteryHistoryFabAction?.value = null
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all battery history?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = graphMode == BatteryGraphMode.SPEED,
                    onClick = { graphMode = BatteryGraphMode.SPEED },
                    label = { Text("Charge/Discharge Speed") },
                    leadingIcon = { Icon(Icons.Default.Speed, null) }
                )
                FilterChip(
                    selected = graphMode == BatteryGraphMode.DRAIN,
                    onClick = { graphMode = BatteryGraphMode.DRAIN },
                    label = { Text("Drain Rate") },
                    leadingIcon = { Icon(Icons.Default.BatteryStd, null) }
                )
            }

            // Graph Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (graphMode == BatteryGraphMode.SPEED) "Current (mA) - All History" else "Drain Rate (%/hr) - All History",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BatteryHistoryGraph(
                        data = historyData,
                        mode = graphMode,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // Stats Card
            BatteryHistoryStatsCard(historyData)
            
            // Legend
            if (graphMode == BatteryGraphMode.DRAIN) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "Active Drain")
                    LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Idle Drain")
                }
            }
            
            // Extra spacer for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

enum class BatteryGraphMode {
    SPEED, DRAIN
}

@Composable
fun BatteryHistoryGraph(
    data: List<BatteryGraphEntry>,
    mode: BatteryGraphMode,
    primaryColor: Color,
    secondaryColor: Color
) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available yet", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val minTime = data.minOf { it.timestamp }
    val maxTime = data.maxOf { it.timestamp }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val (minY, maxY, labelSuffix) = if (mode == BatteryGraphMode.SPEED) {
        val min = data.minOf { it.currentMa }.coerceAtMost(0f)
        val max = data.maxOf { it.currentMa }.coerceAtLeast(0f)
        Triple(min, max, "mA")
    } else {
        val max = data.maxOf { maxOf(it.activeDrainRate, it.idleDrainRate) }.coerceAtLeast(1f)
        Triple(0f, max, "%/hr")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 20.dp)) {
            val width = size.width
            val height = size.height
            val timeRange = (maxTime - minTime).coerceAtLeast(1L)
            val valRange = (maxY - minY).coerceAtLeast(1f)

            if (mode == BatteryGraphMode.SPEED) {
                // Draw Zero Line
                val zeroY = height * (1 - (0f - minY) / valRange)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                val path = Path()
                data.forEachIndexed { index, entry ->
                    val x = width * (entry.timestamp - minTime) / timeRange
                    val y = height * (1 - (entry.currentMa - minY) / valRange)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                // DRAIN MODE
                val activePath = Path()
                val idlePath = Path()
                
                data.forEachIndexed { index, entry ->
                    val x = width * (entry.timestamp - minTime) / timeRange
                    
                    val activeY = height * (1 - entry.activeDrainRate / valRange)
                    if (index == 0) activePath.moveTo(x, activeY) else activePath.lineTo(x, activeY)
                    
                    val idleY = height * (1 - entry.idleDrainRate / valRange)
                    if (index == 0) idlePath.moveTo(x, idleY) else idlePath.lineTo(x, idleY)
                }
                
                drawPath(
                    path = activePath,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = idlePath,
                    color = secondaryColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Labels
        Text(
            text = "${"%.0f".format(maxY)} $labelSuffix",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = "${"%.0f".format(minY)} $labelSuffix",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 20.dp)
        )
        
        // Time Labels
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeFormat.format(Date(minTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeFormat.format(Date(maxTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BatteryHistoryStatsCard(data: List<BatteryGraphEntry>) {
    if (data.isEmpty()) return

    val chargeEntries = data.filter { it.currentMa > 0 }
    val dischargeEntries = data.filter { it.currentMa < 0 }

    val avgCharge = if (chargeEntries.isNotEmpty()) chargeEntries.map { it.currentMa }.average() else 0.0
    val maxCharge = if (chargeEntries.isNotEmpty()) chargeEntries.maxOf { it.currentMa } else 0f
    
    val avgDischarge = if (dischargeEntries.isNotEmpty()) dischargeEntries.map { kotlin.math.abs(it.currentMa) }.average() else 0.0
    val maxDischarge = if (dischargeEntries.isNotEmpty()) dischargeEntries.minOf { it.currentMa }.let { kotlin.math.abs(it) } else 0f

    val avgActiveDrain = if (data.isNotEmpty()) data.map { it.activeDrainRate.toDouble() }.average() else 0.0
    val avgIdleDrain = if (data.isNotEmpty()) data.map { it.idleDrainRate.toDouble() }.average() else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.history_stats_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (avgCharge > 0) {
                StatRow(label = stringResource(R.string.stats_avg_charge_speed), value = "%.0f mA".format(avgCharge))
                StatRow(label = stringResource(R.string.stats_max_charge_speed), value = "%.0f mA".format(maxCharge))
            }
            
            if (avgDischarge > 0) {
                StatRow(label = stringResource(R.string.stats_avg_discharge_speed), value = "%.0f mA".format(avgDischarge))
                StatRow(label = stringResource(R.string.stats_max_discharge_speed), value = "%.0f mA".format(maxDischarge))
            }
            
            StatRow(label = stringResource(R.string.stats_avg_active_drain), value = "%.2f %%/hr".format(avgActiveDrain))
            StatRow(label = stringResource(R.string.stats_avg_idle_drain), value = "%.2f %%/hr".format(avgIdleDrain))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
