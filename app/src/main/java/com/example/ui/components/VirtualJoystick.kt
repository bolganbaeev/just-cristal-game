package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
    accentColor: Color = Color(0xFF00E5FF),
    isAimStick: Boolean = false,
    testTag: String = "joystick",
    onMoved: (dx: Float, dy: Float, angle: Float, isEngaged: Boolean) -> Unit
) {
    var thumbOffset by remember { mutableFloatStateOf(0f) }
    var thumbAngle by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag(testTag)
            .pointerInput(Unit) {
                val maxRadius = size.width / 2.5f
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dragVec = offset - center
                        val dist = hypot(dragVec.x, dragVec.y)
                        val angle = atan2(dragVec.y, dragVec.x)
                        val clampedDist = dist.coerceAtMost(maxRadius)
                        thumbOffset = clampedDist
                        thumbAngle = angle

                        val normX = if (maxRadius > 0) (cos(angle) * (clampedDist / maxRadius)) else 0f
                        val normY = if (maxRadius > 0) (sin(angle) * (clampedDist / maxRadius)) else 0f
                        onMoved(normX, normY, angle, true)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dragVec = change.position - center
                        val dist = hypot(dragVec.x, dragVec.y)
                        val angle = atan2(dragVec.y, dragVec.x)
                        val clampedDist = dist.coerceAtMost(maxRadius)
                        thumbOffset = clampedDist
                        thumbAngle = angle

                        val normX = if (maxRadius > 0) (cos(angle) * (clampedDist / maxRadius)) else 0f
                        val normY = if (maxRadius > 0) (sin(angle) * (clampedDist / maxRadius)) else 0f
                        onMoved(normX, normY, angle, true)
                    },
                    onDragEnd = {
                        isDragging = false
                        thumbOffset = 0f
                        onMoved(0f, 0f, thumbAngle, false)
                    },
                    onDragCancel = {
                        isDragging = false
                        thumbOffset = 0f
                        onMoved(0f, 0f, thumbAngle, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2.2f
            val thumbRadius = size.width / 5.5f

            // Outer Base Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDragging) 0.18f else 0.08f),
                        accentColor.copy(alpha = 0.02f)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = if (isDragging) 0.6f else 0.35f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Crosshair markers if aim stick
            if (isAimStick) {
                drawLine(
                    color = accentColor.copy(alpha = 0.3f),
                    start = Offset(center.x - outerRadius * 0.7f, center.y),
                    end = Offset(center.x + outerRadius * 0.7f, center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.3f),
                    start = Offset(center.x, center.y - outerRadius * 0.7f),
                    end = Offset(center.x, center.y + outerRadius * 0.7f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Draggable Thumb Knob
            val thumbCenter = if (isDragging) {
                Offset(
                    center.x + cos(thumbAngle) * thumbOffset,
                    center.y + sin(thumbAngle) * thumbOffset
                )
            } else {
                center
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.65f)
                    ),
                    center = thumbCenter,
                    radius = thumbRadius
                ),
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = thumbRadius,
                center = thumbCenter,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}
