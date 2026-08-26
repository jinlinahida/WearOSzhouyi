package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.xiaoliuren.XiaoLiuRenEngine
import com.boompala.engine.xiaoliuren.XiaoLiuRenPalace
import com.boompala.engine.xiaoliuren.XiaoLiuRenReading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun XiaoLiuRenScreen(engine: XiaoLiuRenEngine, initial: XiaoLiuRenReading?, rotary: Boolean, onReading: (XiaoLiuRenReading) -> Unit, onArchive: (XiaoLiuRenReading) -> Unit, onBack: () -> Unit) {
    val metrics = LocalUiMetrics.current
    val reading = remember(initial, engine) { initial ?: engine.calculate(Instant.now(), ZoneId.systemDefault()) }
    androidx.compose.runtime.LaunchedEffect(reading) { onReading(reading) }
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA) }
    RotaryScrollColumn(rotaryEnabled = rotary, modifier = Modifier.fillMaxSize(), contentPadding = metrics.screenPadding, itemSpacing = metrics.itemSpacing) {
        item { Text("小六壬") }
        item { ResultCard { DetailField("公历", fmt.format(reading.timeInfo.gregorianDateTime)); DetailField("农历", reading.timeInfo.lunarDate); DetailField("当前时辰", reading.timeInfo.hourGanzhi.earthlyBranch.displayName + "时"); DetailField("起课数据", "月${reading.timeInfo.lunarMonth} 日${reading.timeInfo.lunarDay} 时${reading.timeInfo.hourGanzhi.earthlyBranch.index + 1}") } }
        item { ResultCard { Text("六宫（固定顺序）"); XiaoLiuRenPalace.entries.forEach { p -> Text(if (p == reading.finalPalace) "▶ ${p.displayName}（最终）" else p.displayName) } } }
        item { ResultCard { DetailField("月宫", reading.monthPalace.displayName); DetailField("日宫", reading.dayPalace.displayName); DetailField("时宫/最终", reading.finalPalace.displayName); Text(reading.finalPalace.meaning) } }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { onReading(engine.calculate(Instant.now(), ZoneId.systemDefault())) },
                modifier = Modifier.fillMaxWidth().wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) { Text("重新按当前时间起课") }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { onArchive(reading) },
                modifier = Modifier.fillMaxWidth().wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) { Text("归档此次起课") }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) { Text("返回首页") }
        }
    }
}
