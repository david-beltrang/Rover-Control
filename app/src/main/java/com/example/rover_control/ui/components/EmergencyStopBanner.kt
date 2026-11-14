package com.example.rover_control.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rover_control.R
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.PrimaryCyan

/**
 * Shared E-STOP banner shown on Control and Telemetry. Movement stays locked
 * until the user presses "Rearm" (Implementation_plan.md §6.3).
 */
@Composable
fun EmergencyStopBanner(
    onRearm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ErrorRed.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = ErrorRed
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.control_stopped),
                    style = MaterialTheme.typography.titleSmall,
                    color = ErrorRed,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.control_stopped_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRearm) {
                Text(
                    text = stringResource(R.string.control_rearm),
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
