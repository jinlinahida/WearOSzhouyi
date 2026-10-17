package com.boompala.ui.pulse

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.jinlinahida.shirokowear.ui.ShirokoWearLiveWaveCanvas
import io.github.jinlinahida.shirokowear.ui.ShirokoWearStripWaveCanvas
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.UnstableShirokoWearApi

/**
 * 测量中专用的 C 位发光脉冲线 Canvas。
 * 转发至 ShirokoWear UI 的 [ShirokoWearLiveWaveCanvas]。
 */
@OptIn(UnstableShirokoWearApi::class)
@Composable
fun PulseWaveCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ShirokoWearTheme.colors.waveformLine,
) {
    ShirokoWearLiveWaveCanvas(
        points = points,
        modifier = modifier,
        lineColor = lineColor,
    )
}

/**
 * 结果页专用的脉象矢量波形图 Canvas。
 * 转发至 ShirokoWear UI 的 [ShirokoWearStripWaveCanvas]。
 */
@OptIn(UnstableShirokoWearApi::class)
@Composable
fun ReferenceWaveformCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ShirokoWearTheme.colors.waveformLine,
) {
    ShirokoWearStripWaveCanvas(
        points = points,
        modifier = modifier,
        lineColor = lineColor,
    )
}
