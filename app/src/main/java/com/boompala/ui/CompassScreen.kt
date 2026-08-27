package com.boompala.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.compass.*
import java.time.LocalDate
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(rotaryScrollingEnabled: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var sensorState by remember { mutableStateOf(SensorCompassState()) }
    var lockedHeading by remember { mutableStateOf<Float?>(null) }
    val controller = remember(context, lifecycleOwner) {
        CompassSensorController(
            manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
            display = context.display,
            onState = { sensorState = it },
        )
    }
    DisposableEffect(lifecycleOwner, controller) {
        lifecycleOwner.lifecycle.addObserver(controller)
        onDispose { lifecycleOwner.lifecycle.removeObserver(controller); controller.stop() }
    }

    val locked = lockedHeading != null
    val visibleHeading = lockedHeading ?: sensorState.heading
    val reading = visibleHeading?.let(CompassMath::reading)
    val period = remember { YuanYunData.periodFor(LocalDate.now()) }
    val statusText = sensorStatusText(sensorState, locked)
    val metrics = LocalUiMetrics.current

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "dial") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ModernLuoPan(reading)
                Text(
                    text = reading?.let { String.format(Locale.US, "%.1f°  %s · 磁北", it.degrees, it.eightDirection) }
                        ?: "等待磁北方向数据",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(statusText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
        item(key = "reading-card") {
            ResultCard {
                Text("当前方位", style = MaterialTheme.typography.titleSmall)
                DetailField("精确角度", reading?.let { String.format(Locale.US, "%.1f° · %s", it.degrees, it.eightDirection) } ?: "暂无")
                DetailField("朝向山位", reading?.let { "${it.mountain.name}山 · ${it.mountain.element} · ${it.mountain.yinYang.displayName}" } ?: "暂无")
                DetailField("坐山朝向", reading?.let { "坐${it.sittingMountain.name}山，向${it.mountain.name}山" } ?: "暂无")
                DetailField("后天八卦", reading?.let { "${it.trigram.symbol}${it.trigram.name}卦 · ${it.trigram.direction} · ${it.trigram.element}" } ?: "暂无")
            }
        }
        item(key = "period-card") {
            ResultCard {
                Text("元运九星", style = MaterialTheme.typography.titleSmall)
                DetailField("当前元运", "${period.displayName}（${period.startYear}–${period.endYear}）")
                DetailField("当运主星", "${period.rulingStar.displayName} · ${period.rulingStar.element} · ${period.rulingStar.trigram}宫")
                reading?.let {
                    val palaceStar = YuanYunData.stars[it.trigram.luoShuNumber - 1]
                    DetailField("本方原宫", "${palaceStar.displayName} · ${YuanYunData.status(palaceStar.number, period.number)}")
                    Text("简评：${palaceStar.neutralMeaning}。${YuanYunData.periodSummary(period)}。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item(key = "lock") {
            val lockInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { if (locked) lockedHeading = null else lockedHeading = sensorState.heading },
                enabled = sensorState.available && (sensorState.heading != null || locked),
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(lockInteraction),
                interactionSource = lockInteraction,
            ) { Text(if (locked) "恢复实时测量" else "锁定当前方位") }
        }
        item(key = "calibration") {
            ResultCard {
                Text("校准与使用", style = MaterialTheme.typography.titleSmall)
                Text("尽量水平持表，远离磁扣、扬声器和金属桌面；精度较低时缓慢做“8”字动作。", style = MaterialTheme.typography.bodySmall)
            }
        }
        item(key = "back") {
            val backInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(backInteraction),
                interactionSource = backInteraction,
            ) { Text("返回首页") }
        }
    }
}

private fun sensorStatusText(state: SensorCompassState, locked: Boolean): String = when {
    !state.available -> state.message ?: "传感器不可用"
    locked -> "已锁定当前读数"
    state.tilted -> "倾斜过大 · 请尽量水平持表"
    state.magneticInterference -> "磁场异常 · 请远离磁性物体"
    state.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "传感器精度高"
    state.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "传感器精度中"
    else -> "精度较低 · 建议校准"
}

@Composable
private fun ModernLuoPan(reading: CompassReading?) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val muted = foreground.copy(alpha = .44f)
    val hairline = foreground.copy(alpha = .16f)
    val purple = MaterialTheme.colorScheme.primary
    val dialBackground = Color(0xFF17131D)
    val heading = reading?.degrees ?: 0f
    val mountainIndex = reading?.let { CompassMath.mountains.indexOf(it.mountain) } ?: -1
    val trigramIndex = reading?.let { CompassMath.trigrams.indexOf(it.trigram) } ?: -1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .background(dialBackground, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .graphicsLayer { rotationZ = -heading },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f
            val baguaInner = radius * .29f
            val baguaOuter = radius * .49f
            val mountainOuter = radius * .71f
            val degreeOuter = radius * .96f

            if (trigramIndex >= 0) {
                val start = CompassMath.trigrams[trigramIndex].centerDegrees - 112.5f
                val mid = (baguaInner + baguaOuter) / 2f
                drawArc(purple.copy(alpha = .16f), start, 45f, false, topLeft(center, mid), Size(mid * 2, mid * 2), style = Stroke(baguaOuter - baguaInner))
            }
            if (mountainIndex >= 0) {
                val start = CompassMath.mountains[mountainIndex].centerDegrees - 97.5f
                val mid = (baguaOuter + mountainOuter) / 2f
                drawArc(purple.copy(alpha = .24f), start, 15f, false, topLeft(center, mid), Size(mid * 2, mid * 2), style = Stroke(mountainOuter - baguaOuter))
            }

            listOf(baguaInner, baguaOuter, mountainOuter, degreeOuter).forEachIndexed { index, r ->
                drawCircle(if (index == 3) foreground.copy(alpha = .65f) else hairline, r, center, style = Stroke(if (index == 3) 1.4.dp.toPx() else .7.dp.toPx()))
            }

            val baguaPaint = dialPaint(foreground, 10.dp.toPx(), Typeface.BOLD)
            CompassMath.trigrams.forEachIndexed { index, trigram ->
                val p = radialPoint(center, radius * .40f, trigram.centerDegrees.toFloat())
                baguaPaint.color = (if (index == trigramIndex) purple else foreground).toArgb()
                drawCenteredText("${trigram.symbol}${trigram.name}", p, baguaPaint)
                val boundary = radialLine(center, baguaInner, baguaOuter, trigram.centerDegrees - 22.5f)
                drawLine(hairline, boundary.first, boundary.second, .7.dp.toPx())
            }

            val mountainPaint = dialPaint(foreground, 10.dp.toPx(), Typeface.BOLD)
            CompassMath.mountains.forEachIndexed { index, mountain ->
                val p = radialPoint(center, radius * .60f, mountain.centerDegrees.toFloat())
                mountainPaint.color = (if (index == mountainIndex) purple else foreground.copy(alpha = .86f)).toArgb()
                drawCenteredText(mountain.name, p, mountainPaint)
                val boundary = radialLine(center, baguaOuter, mountainOuter, mountain.startDegrees)
                drawLine(hairline, boundary.first, boundary.second, .6.dp.toPx())
            }

            val degreePaint = dialPaint(foreground, 8.dp.toPx(), Typeface.BOLD)
            for (degree in 0 until 360 step 5) {
                val long = degree % 30 == 0
                val medium = degree % 10 == 0
                val line = radialLine(center, radius * if (long) .83f else if (medium) .87f else .91f, degreeOuter, degree.toFloat())
                drawLine(if (long) foreground else muted, line.first, line.second, if (long) 1.3.dp.toPx() else .6.dp.toPx())
                if (long) {
                    val label = when (degree) { 0 -> "北 0"; 90 -> "东 90"; 180 -> "南 180"; 270 -> "西 270"; else -> degree.toString() }
                    val p = radialPoint(center, radius * .78f, degree.toFloat())
                    degreePaint.color = (if (degree % 90 == 0) purple else foreground).toArgb()
                    drawCenteredText(label, p, degreePaint)
                }
            }

            // Magnetic needle is part of the rotating instrument layer: its violet tip always points magnetic north.
            drawLine(purple, center, radialPoint(center, radius * .23f, 0f), 4.dp.toPx())
            drawLine(muted, center, radialPoint(center, radius * .19f, 180f), 3.dp.toPx())
            drawCircle(dialBackground, radius * .055f, center)
            drawCircle(purple, radius * .026f, center)
        }

        // Tianxin crosshairs stay fixed to the watch and define facing (top) and sitting (bottom).
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f
            drawLine(foreground.copy(alpha = .30f), Offset(center.x - radius * .97f, center.y), Offset(center.x + radius * .97f, center.y), .7.dp.toPx())
            drawLine(purple.copy(alpha = .78f), Offset(center.x, center.y - radius * .98f), Offset(center.x, center.y + radius * .98f), 1.dp.toPx())
            drawCircle(foreground.copy(alpha = .28f), radius * .29f, center, style = Stroke(.8.dp.toPx()))
            val marker = 5.dp.toPx()
            drawLine(purple, Offset(center.x - marker, center.y - radius * .98f), Offset(center.x, center.y - radius * .91f), 2.dp.toPx())
            drawLine(purple, Offset(center.x + marker, center.y - radius * .98f), Offset(center.x, center.y - radius * .91f), 2.dp.toPx())
        }
        Text("向", color = purple, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopCenter).padding(top = 1.dp))
        Text("坐", color = foreground.copy(alpha = .62f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp))
    }
}

private fun dialPaint(color: Color, textSize: Float, style: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color.toArgb()
    this.textSize = textSize
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, style)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredText(text: String, point: Offset, paint: Paint) {
    val baseline = point.y - (paint.ascent() + paint.descent()) / 2f
    drawIntoCanvas { it.nativeCanvas.drawText(text, point.x, baseline, paint) }
}

private fun radialPoint(center: Offset, radius: Float, bearing: Float): Offset {
    val radians = Math.toRadians((bearing - 90f).toDouble())
    return Offset(center.x + cos(radians).toFloat() * radius, center.y + sin(radians).toFloat() * radius)
}

private fun radialLine(center: Offset, inner: Float, outer: Float, bearing: Float) =
    radialPoint(center, inner, bearing) to radialPoint(center, outer, bearing)

private fun topLeft(center: Offset, radius: Float) = Offset(center.x - radius, center.y - radius)
