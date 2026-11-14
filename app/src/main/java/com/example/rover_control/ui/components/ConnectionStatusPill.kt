package com.example.rover_control.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.rover_control.R
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.SuccessGreen
import com.example.rover_control.ui.theme.WarningAmber

/**
 * Compact connection indicator for top app bars (Implementation_plan.md §6.1).
 * Green = connected, amber = connecting/demo, red = disconnected.
 */
@Composable
fun ConnectionStatusPill(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color: Color
    val label: String
    when (state) {
        is ConnectionState.Connected -> {
            if (state.isDemo) {
                color = WarningAmber
                label = stringResource(R.string.status_demo)
            } else {
                color = SuccessGreen
                label = stringResource(R.string.connection_connected)
            }
        }

        is ConnectionState.Connecting -> {
            color = WarningAmber
            label = stringResource(R.string.status_connecting)
        }

        is ConnectionState.Disconnected -> {
            color = ErrorRed
            label = stringResource(R.string.status_disconnected)
        }
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}
