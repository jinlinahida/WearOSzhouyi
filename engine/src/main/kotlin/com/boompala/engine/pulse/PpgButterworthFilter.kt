package com.boompala.engine.pulse

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 二阶双二阶（Biquad）IIR 滤波器实现。
 * 支持高通 (消除呼吸基线漂移) 与低通 (消除高频肌电与环境光噪点)，可串联成带通滤波器。
 */
class BiquadFilter(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    /**
     * 重置滤波器历史状态。
     */
    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    /**
     * 流式单点滤波（因果滤波，适用于实时 Canvas 波形走带）。
     */
    fun process(sample: Double): Double {
        val y0 = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = sample
        y2 = y1
        y1 = y0
        return y0
    }

    companion object {
        /**
         * 构建标准二阶高通巴特沃斯滤波器。
         */
        fun highPass(cutoffHz: Double, sampleRateHz: Double, q: Double = 1.0 / sqrt(2.0)): BiquadFilter {
            val omega = 2.0 * PI * (cutoffHz / sampleRateHz)
            val cosOmega = cos(omega)
            val alpha = sin(omega) / (2.0 * q)

            val a0 = 1.0 + alpha
            val b0 = ((1.0 + cosOmega) / 2.0) / a0
            val b1 = (-(1.0 + cosOmega)) / a0
            val b2 = ((1.0 + cosOmega) / 2.0) / a0
            val a1 = (-2.0 * cosOmega) / a0
            val a2 = (1.0 - alpha) / a0

            return BiquadFilter(b0, b1, b2, a1, a2)
        }

        /**
         * 构建标准二阶低通巴特沃斯滤波器。
         */
        fun lowPass(cutoffHz: Double, sampleRateHz: Double, q: Double = 1.0 / sqrt(2.0)): BiquadFilter {
            val omega = 2.0 * PI * (cutoffHz / sampleRateHz)
            val cosOmega = cos(omega)
            val alpha = sin(omega) / (2.0 * q)

            val a0 = 1.0 + alpha
            val b0 = ((1.0 - cosOmega) / 2.0) / a0
            val b1 = (1.0 - cosOmega) / a0
            val b2 = ((1.0 - cosOmega) / 2.0) / a0
            val a1 = (-2.0 * cosOmega) / a0
            val a2 = (1.0 - alpha) / a0

            return BiquadFilter(b0, b1, b2, a1, a2)
        }
    }
}

/**
 * 专为 PPG 脉搏波设计的带通滤波器（通带 0.5Hz ~ 8.0Hz，对应心率 30 ~ 480 BPM）。
 */
class PpgButterworthFilter(
    val sampleRateHz: Double = 25.0,
    highPassCutoffHz: Double = 0.5,
    lowPassCutoffHz: Double = 8.0,
) {
    private val hp = BiquadFilter.highPass(highPassCutoffHz, sampleRateHz)
    private val lp = BiquadFilter.lowPass(
        // 当采样率为 25Hz 时，Nyquist 频率为 12.5Hz，低通截止应限制在 8.0Hz 内
        cutoffHz = lowPassCutoffHz.coerceAtMost(sampleRateHz * 0.45),
        sampleRateHz = sampleRateHz,
    )

    /**
     * 流式单点滤波（适用于 Wear OS 实时接收 PPG 渲染）。
     */
    fun process(sample: Double): Double {
        val hpOut = hp.process(sample)
        return lp.process(hpOut)
    }

    /**
     * 重置状态。
     */
    fun reset() {
        hp.reset()
        lp.reset()
    }

    /**
     * 批量双向零相位滤波（Filtfilt），先去基线直流分量，再进行正反双向滤波消除相位延迟。
     */
    fun filterBatch(samples: DoubleArray): DoubleArray {
        if (samples.size < 4) return samples.clone()

        // 1. 去直流均值分量，规避初态阶跃震荡
        val mean = samples.average()
        val centered = DoubleArray(samples.size) { samples[it] - mean }

        // 2. 正向滤波
        reset()
        val forward = DoubleArray(centered.size)
        for (i in centered.indices) {
            forward[i] = process(centered[i])
        }

        // 3. 反向滤波（消除相位偏移）
        reset()
        val backward = DoubleArray(centered.size)
        for (i in centered.indices.reversed()) {
            backward[i] = process(forward[i])
        }

        return backward
    }
}
