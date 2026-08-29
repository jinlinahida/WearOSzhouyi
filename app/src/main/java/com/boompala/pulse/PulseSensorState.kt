package com.boompala.pulse

import com.boompala.engine.pulse.PulseDiagnosisResult

/**
 * 把脉/切脉测量会话的状态机模型。
 */
sealed class PulseSensorState {
    /** 初始闲置状态 */
    object Idle : PulseSensorState()

    /** 正在准备/感应手腕微血管搏动 */
    object Preparing : PulseSensorState()

    /** 未佩戴或未紧贴手腕（脱腕拦截） */
    object NotWorn : PulseSensorState()

    /**
     * 把脉进行中（由真实传感器心搏事件驱动物理波形）。
     * @param progress 测量进度 (0f..1f)
     * @param wavePoints 供 UI Canvas 绘制的归一化波形点序列 (0f..1f)
     * @param currentBpm 当前瞬时测得的心率 (可为 null)
     * @param isPeakNow 是否刚刚识别到一个真实波峰（用于驱动表盘马达微震动）
     */
    data class Measuring(
        val progress: Float,
        val wavePoints: List<Float>,
        val currentBpm: Double? = null,
        val isPeakNow: Boolean = false,
    ) : PulseSensorState()

    /** 测量完毕，算法正在进行 HRV 时域与中医脉象辨析 */
    object Analyzing : PulseSensorState()

    /** 分析完成，输出权威诊断报告 */
    data class Completed(
        val result: PulseDiagnosisResult,
    ) : PulseSensorState()

    /**
     * 测量数据质量与覆盖率不足（如手腕剧烈晃动、中途漏光），要求用户重新把脉。
     * @param reason 质检未达标原因
     */
    data class QualityFailed(
        val reason: String,
    ) : PulseSensorState()

    /**
     * 测量异常。
     * @param reason 错误原因标识
     * @param canFallback 是否可降级
     */
    data class Error(
        val reason: String,
        val canFallback: Boolean = false,
    ) : PulseSensorState()
}
