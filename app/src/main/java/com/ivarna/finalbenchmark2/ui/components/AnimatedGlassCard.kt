package com.ivarna.finalbenchmark2.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    borderColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
    delayMillis: Int = 0,
    animationDuration: Int = 0,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor,
        onClick = onClick,
        content = content
    )
}
