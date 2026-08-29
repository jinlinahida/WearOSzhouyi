package com.boompala.ui.pulse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 测量中专用的 C 位发光脉冲线 Canvas。
 * 使用三阶贝塞尔平滑连接各采样点，双向边缘渐变渐隐，外层携带激光般的多层呼吸微光。
 */
@Composable
fun PulseWaveCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00E5A3), // 翡翠微光青
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val centerY = h * 0.50f
        val stepX = w / (points.size - 1)

        val path = Path()

        // 1. 三阶贝塞尔曲线平滑连接采样点，杜绝生硬折线
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val x0 = i * stepX
            val y0 = centerY - (p0 - 0.5f) * (h * 0.55f)
            val x1 = (i + 1) * stepX
            val y1 = centerY - (p1 - 0.5f) * (h * 0.55f)

            if (i == 0) path.moveTo(x0, y0)
            val cx = (x0 + x1) / 2f
            path.cubicTo(cx, y0, cx, y1, x1, y1)
        }

        // 2. 双向水平渐隐画刷：左侧平滑融于纯黑夜色，右侧最明亮
        val glowBrush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.15f to lineColor.copy(alpha = 0.20f),
            0.80f to lineColor.copy(alpha = 0.50f),
            1.0f to Color.White.copy(alpha = 0.90f),
        )
        val coreBrush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.18f to lineColor.copy(alpha = 0.50f),
            0.85f to lineColor.copy(alpha = 0.95f),
            1.0f to Color.White,
        )

        // 3. 外层激光微光晕（Glow Bloom）
        drawPath(
            path = path,
            brush = glowBrush,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        )

        // 4. 核心高精脉络线
        drawPath(
            path = path,
            brush = coreBrush,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )

        // 5. 脉冲前沿发光微核（Energy Nucleus）
        val lastIdx = points.lastIndex
        val lastX = lastIdx * stepX
        val lastY = centerY - (points[lastIdx] - 0.5f) * (h * 0.55f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, lineColor.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(lastX, lastY),
                radius = 7.dp.toPx(),
            ),
            radius = 7.dp.toPx(),
            center = Offset(lastX, lastY),
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(lastX, lastY),
        )
    }
}

/**
 * 结果页专用的典型脉象矢量波形图 Canvas。
 * 紧凑优雅地展示单个心动周期的主波、切迹与重搏波特征。
 */
@Composable
fun ReferenceWaveformCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00E5A3),
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val paddingV = h * 0.15f
        val availableH = h - (paddingV * 2)
        val stepX = w / (points.size - 1)

        val path = Path()
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val x0 = i * stepX
            val y0 = h - paddingV - (p0 * availableH)
            val x1 = (i + 1) * stepX
            val y1 = h - paddingV - (p1 * availableH)

            if (i == 0) path.moveTo(x0, y0)
            val cx = (x0 + x1) / 2f
            path.cubicTo(cx, y0, cx, y1, x1, y1)
        }

        // 外层微光
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.35f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
        )
        // 核心线
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
