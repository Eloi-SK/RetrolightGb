package com.eloi.retrolightgb.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eloi.retrolightgb.core.memory.JoypadButton
import org.jetbrains.compose.ui.tooling.preview.Preview

private val DPadColor = Color(0xFF2A2A2A)
private val ActionButtonColor = Color(0xFF8B2252)
private val MenuButtonColor = Color(0xFF5A5A5A)

@Composable
fun MobileJoypad(
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DPad(onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased)
            ActionButtons(onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased)
        }
        MenuButtons(onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased)
    }
}

@Composable
private fun DPad(
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
) {
    val armSize = 44.dp
    val crossSize = armSize * 3

    Box(modifier = Modifier.size(crossSize)) {
        Box(
            modifier = Modifier
                .width(crossSize).height(armSize)
                .align(Alignment.Center)
                .background(DPadColor, RoundedCornerShape(6.dp))
        )
        Box(
            modifier = Modifier
                .width(armSize).height(crossSize)
                .align(Alignment.Center)
                .background(DPadColor, RoundedCornerShape(6.dp))
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center)
                .background(Color(0xFF3C3C3C), CircleShape)
        )

        DPadArm(
            button = JoypadButton.Up, icon = Icons.Filled.KeyboardArrowUp,
            modifier = Modifier.size(armSize).align(Alignment.TopCenter),
            onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased,
        )
        DPadArm(
            button = JoypadButton.Down, icon = Icons.Filled.KeyboardArrowDown,
            modifier = Modifier.size(armSize).align(Alignment.BottomCenter),
            onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased,
        )
        DPadArm(
            button = JoypadButton.Left, icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            modifier = Modifier.size(armSize).align(Alignment.CenterStart),
            onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased,
        )
        DPadArm(
            button = JoypadButton.Right, icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = Modifier.size(armSize).align(Alignment.CenterEnd),
            onButtonPressed = onButtonPressed, onButtonReleased = onButtonReleased,
        )
    }
}

@Composable
private fun DPadArm(
    button: JoypadButton,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onButtonPressed(button)
        } else {
            onButtonReleased(button)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {},
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (isPressed) 0.4f else 0.8f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ActionButtons(
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
) {
    // B bottom-left, A top-right — mirrors the physical Game Boy layout
    Box(modifier = Modifier.size(110.dp, 92.dp)) {
        ActionButton(
            text = "B",
            modifier = Modifier.size(52.dp).align(Alignment.BottomStart),
            onButtonPressed = { onButtonPressed(JoypadButton.B) },
            onButtonReleased = { onButtonReleased(JoypadButton.B) },
        )
        ActionButton(
            text = "A",
            modifier = Modifier.size(52.dp).align(Alignment.TopEnd),
            onButtonPressed = { onButtonPressed(JoypadButton.A) },
            onButtonReleased = { onButtonReleased(JoypadButton.A) },
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onButtonPressed: () -> Unit,
    onButtonReleased: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onButtonPressed()
        } else {
            onButtonReleased()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = if (isPressed) ActionButtonColor.copy(alpha = 0.55f) else ActionButtonColor,
                shape = CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun MenuButtons(
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        MenuButton(
            text = "SELECT",
            onButtonPressed = { onButtonPressed(JoypadButton.Select) },
            onButtonReleased = { onButtonReleased(JoypadButton.Select) },
        )
        MenuButton(
            text = "START",
            onButtonPressed = { onButtonPressed(JoypadButton.Start) },
            onButtonReleased = { onButtonReleased(JoypadButton.Start) },
        )
    }
}

@Composable
private fun MenuButton(
    text: String,
    onButtonPressed: () -> Unit,
    onButtonReleased: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onButtonPressed()
        } else {
            onButtonReleased()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 80.dp, height = 26.dp)
            .background(
                color = if (isPressed) MenuButtonColor.copy(alpha = 0.55f) else MenuButtonColor,
                shape = RoundedCornerShape(50),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
@Preview
fun MobileJoyPadPreview() {
    MobileJoypad(onButtonPressed = {}, onButtonReleased = {})
}