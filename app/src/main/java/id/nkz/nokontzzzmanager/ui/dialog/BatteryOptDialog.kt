package id.nkz.nokontzzzmanager.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.nkz.nokontzzzmanager.R

@Composable
fun BatteryOptDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onExit: () -> Unit,
    showExitButton: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal by tapping outside */ },
        title = { Text(if (showExitButton) stringResource(R.string.permissions_required) else stringResource(R.string.battery_optimization)) },
        text = {
            Text(
                if (showExitButton) {
                    stringResource(R.string.battery_opt_desc_exit)
                } else {
                    stringResource(R.string.battery_opt_desc_later)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            if (showExitButton) {
                TextButton(onClick = onExit) {
                    Text(stringResource(R.string.exit_app_dialog))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.later))
                }
            }
        }
    )
}
