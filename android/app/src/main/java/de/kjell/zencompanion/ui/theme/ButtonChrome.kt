package de.kjell.zencompanion.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Flat hairline, just above the lift fill. Same 0.08 as iOS `glassRim`. */
@Composable
internal fun Modifier.liftRim(shape: Shape): Modifier {
    val zen = LocalZenColors.current
    val line = (if (zen.isDark) Color.White else Color.Black).copy(alpha = 0.08f)
    return this.border(width = 0.5.dp, color = line, shape = shape)
}

/**
 * Shrinks and dims while the finger is down. Pass the same source to the clickable.
 */
@Composable
internal fun Modifier.pressFeedback(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "pressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.58f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "pressAlpha",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
