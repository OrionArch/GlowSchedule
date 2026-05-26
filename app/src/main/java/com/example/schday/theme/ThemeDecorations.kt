package com.example.schday.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.composed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

/**
 * Custom glow modifier for AMOLED Pop neon cards or warm shadow for Vintage Library cards.
 */
fun Modifier.glowOrShadow(appTheme: GlowTheme, isFeatured: Boolean = false, glowColor: Color = Color.Unspecified): Modifier = this.drawBehind {
    if (appTheme == GlowTheme.AMOLED_POP && isFeatured) {
        val color = if (glowColor != Color.Unspecified) glowColor else Color(0x3300FF66)
        // Draw concentric glowing strokes to simulate blur
        for (i in 1..4) {
            val spread = i * 2.dp.toPx()
            val alpha = 0.15f / i
            drawRoundRect(
                color = color.copy(alpha = alpha),
                size = size.copy(width = size.width + spread * 2, height = size.height + spread * 2),
                topLeft = Offset(-spread, -spread),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = spread)
            )
        }
    } else if (appTheme == GlowTheme.VINTAGE_LIBRARY) {
        // Soft drop shadow with warm brown tint
        drawRoundRect(
            color = Color(0x1F1A1410),
            size = size,
            topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

/**
 * Organic paper texture overlay for Vintage Library cards.
 */
fun Modifier.paperTexture(appTheme: GlowTheme): Modifier = this.drawWithContent {
    drawContent()
    if (appTheme == GlowTheme.VINTAGE_LIBRARY) {
        val random = java.util.Random(1337) // Stable static seed
        // Draw light and dark speckles simulating natural heavy stock paper
        val speckleCount = (size.width * size.height / 1000).toInt().coerceIn(30, 300)
        for (i in 0 until speckleCount) {
            val rx = random.nextFloat() * size.width
            val ry = random.nextFloat() * size.height
            val radius = (0.4f + random.nextFloat() * 0.8f).dp.toPx()
            val alpha = 0.02f + random.nextFloat() * 0.05f
            val isDark = random.nextBoolean()
            val color = if (isDark) Color(0xFF5D3F22) else Color(0xFFFFFFFF)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(rx, ry)
            )
        }
    }
}

/**
 * Technical grid background for cyberpunk HUD feel on AMOLED Pop.
 */
fun Modifier.hudBackground(appTheme: GlowTheme): Modifier = this.drawBehind {
    if (appTheme == GlowTheme.AMOLED_POP) {
        val step = 36.dp.toPx()
        val gridColor = Color(0x0500FF66) // Extremely faint Neon Green
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
            x += step
        }
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
            y += step
        }
    }
}

/**
 * Custom divider that styles itself dynamically according to the active theme.
 */
@Composable
fun GlowDivider(
    appTheme: GlowTheme,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val defaultColor = MaterialTheme.colorScheme.outlineVariant
    val finalColor = if (color != Color.Unspecified) color else defaultColor

    when (appTheme) {
        GlowTheme.AMOLED_POP -> {
            // Neon dotted/dashed horizontal rule
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
                val dotWidth = 4.dp.toPx()
                val spaceWidth = 4.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawRect(
                        color = primaryColor.copy(alpha = 0.4f),
                        topLeft = Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(dotWidth, 1.dp.toPx())
                    )
                    x += dotWidth + spaceWidth
                }
            }
        }
        GlowTheme.VINTAGE_LIBRARY -> {
            // Elegant paper divider with a diamond glyph center
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(16.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    color = finalColor.copy(alpha = 0.4f),
                    thickness = 0.8.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◆",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = finalColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
        else -> {
            // Clean standard divider
            HorizontalDivider(
                color = finalColor.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = modifier
            )
        }
    }
}

/**
 * Bounce clickable modifier that squashes when pressed and springs back when released.
 */
fun Modifier.bounceClickable(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    onClick: () -> Unit
): Modifier = this.composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default grey ripple to highlight bounce
            onClick = onClick
        )
}

/**
 * 3D Holographic Card Tilt that tilts the card when dragged and springs back when released.
 */
fun Modifier.interactiveTilt(appTheme: GlowTheme): Modifier = this.composed {
    if (appTheme == GlowTheme.ACADEMIC_SERENITY) return@composed this

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val tiltX by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val tiltY by animateFloatAsState(
        targetValue = -offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    this
        .pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x / 10f).coerceIn(-12f, 12f)
                    offsetY = (offsetY + dragAmount.y / 10f).coerceIn(-12f, 12f)
                },
                onDragEnd = {
                    offsetX = 0f
                    offsetY = 0f
                },
                onDragCancel = {
                    offsetX = 0f
                    offsetY = 0f
                }
            )
        }
        .graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            cameraDistance = 16f * density
        }
}

