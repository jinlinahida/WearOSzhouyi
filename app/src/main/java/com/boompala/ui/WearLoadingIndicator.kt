package com.boompala.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * Google 官方标准 Material 3 Expressive 形状变形加载动画组件。
 *
 * 严格遵循 Material 3 Expressive 规范，在标准圆角四芒星、正圆、圆角八角花形、平滑四边形之间
 * 进行平滑无缝流变（Morphing），配合轻微低速微自转，呈现纯粹原厂几何美学。
 */
@Composable
fun ExpressiveShapeMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    animationsEnabled: Boolean = true,
) {
    // 1. Google 官方标准 Material 3 形状序列
    val shape1Sparkle = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.5f,
            rounding = CornerRounding(0.2f),
        )
    }
    val shape2Circle = remember {
        RoundedPolygon.circle(numVertices = 16)
    }
    val shape3Scallop = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.75f,
            rounding = CornerRounding(0.15f),
        )
    }
    val shape4Squircle = remember {
        RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(0.35f),
        )
    }

    // 2. 依次构建连续变形管道：1 -> 2 -> 3 -> 4 -> 1
    val morphs = remember {
        listOf(
            Morph(shape1Sparkle, shape2Circle),
            Morph(shape2Circle, shape3Scallop),
            Morph(shape3Scallop, shape4Squircle),
            Morph(shape4Squircle, shape1Sparkle),
        )
    }

    // 3. 动画驱动：单次完整大循环 2400ms，对应 4 个阶段（每阶段 600ms）
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_shape_morph")
    val rawProgress by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "morph_progress",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // 自转角度：配合变形平滑慢速旋转 360 度
    val rotationDegrees = rawProgress * 90f

    // 4. 当前形态插值计算
    val stage = rawProgress.toInt().coerceIn(0, 3)
    val stageFraction = rawProgress - stage
    // 遵循 Material 标准 Emphasized 缓动曲线
    val morphProgress = FastOutSlowInEasing.transform(stageFraction)
    val currentMorph = morphs[stage]

    Canvas(modifier = modifier.size(size)) {
        val minDim = this.size.minDimension
        val scaleRadius = (minDim / 2f) * 0.9f
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f

        val path = currentMorph.toPath(progress = morphProgress).asComposePath()

        withTransform({
            translate(left = centerX, top = centerY)
            rotate(degrees = rotationDegrees, pivot = Offset.Zero)
            scale(scaleX = scaleRadius, scaleY = scaleRadius, pivot = Offset.Zero)
        }) {
            drawPath(path = path, color = color)
        }
    }
}

/**
 * Wear OS 统一加载页，居中呈现 Google 官方标准 Expressive 变形加载动画及提示标签。
 */
@Composable
fun WearLoadingIndicator(
    label: String,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true,
) {
    val metrics = LocalUiMetrics.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(metrics.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ExpressiveShapeMorphingLoader(
                size = 38.dp,
                animationsEnabled = animationsEnabled,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
