package com.eloi.retrolightgb.ui.mobile

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eloi.retrolightgb.core.memory.JoypadButton
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MobileJoypad(
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // D-Pad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JoypadKey(
                    icon = Icons.Filled.KeyboardArrowUp,
                    onButtonPressed = { onButtonPressed(JoypadButton.Up) },
                    onButtonReleased = { onButtonReleased(JoypadButton.Up) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JoypadKey(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        onButtonPressed = { onButtonPressed(JoypadButton.Left) },
                        onButtonReleased = { onButtonReleased(JoypadButton.Left) },
                    )
                    JoypadKey(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onButtonPressed = { onButtonPressed(JoypadButton.Right) },
                        onButtonReleased = { onButtonReleased(JoypadButton.Right) },
                    )
                }
                JoypadKey(
                    icon = Icons.Filled.KeyboardArrowDown,
                    onButtonPressed = { onButtonPressed(JoypadButton.Down) },
                    onButtonReleased = { onButtonReleased(JoypadButton.Down) },
                )
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JoypadKey(
                    text = "B",
                    onButtonPressed = { onButtonPressed(JoypadButton.B) },
                    onButtonReleased = { onButtonReleased(JoypadButton.B) },
                )
                JoypadKey(
                    text = "A",
                    onButtonPressed = { onButtonPressed(JoypadButton.A) },
                    onButtonReleased = { onButtonReleased(JoypadButton.A) },
                )
            }
        }

        // Select and Start
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            JoypadKey(
                text = "Select",
                onButtonPressed = { onButtonPressed(JoypadButton.Select) },
                onButtonReleased = { onButtonReleased(JoypadButton.Select) },
            )
            JoypadKey(
                text = "Start",
                onButtonPressed = { onButtonPressed(JoypadButton.Start) },
                onButtonReleased = { onButtonReleased(JoypadButton.Start) },
            )
        }
    }
}

@Composable
private fun JoypadKey(
    text: String? = null,
    icon: ImageVector? = null,
    onButtonPressed: () -> Unit,
    onButtonReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onButtonPressed()
        } else {
            onButtonReleased()
        }
    }

    Button(
        onClick = {},
        interactionSource = interactionSource,
        modifier = modifier
    ) {
        if (text != null) Text(text)
        if (icon != null) Icon(icon, contentDescription = null)
    }
}

@Composable
@Preview
fun MobileJoyPadPreview() {
    MobileJoypad(onButtonPressed = {}, onButtonReleased = {})
}
