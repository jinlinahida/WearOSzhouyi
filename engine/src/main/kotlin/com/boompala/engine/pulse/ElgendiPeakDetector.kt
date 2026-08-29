package com.boompala.engine.pulse

import kotlin.math.roundToInt

/**
 * Elgendi 双事件移动均线脉搏波波峰检测算法。
 * 业界公认对光电容积脉搏波（PPG）极具抗噪性与轻量高效的实时/批处理波峰识别算法。
 */
object ElgendiPeakDetector {

    /**
     * 检测信号中的主波峰（Systolic Peaks）索引序列。
     *
     * @param signal 已滤波去噪的 PPG 数组
     * @param sampleRateHz 采样率（如 25.0Hz 或 100.0Hz）
     * @return 波峰在原始数组中的下标索引列表
     */
    fun findPeaks(signal: DoubleArray, sampleRateHz: Double = 25.0): List<Int> {
        if (signal.size < sampleRateHz * 1.5) return emptyList()

        // 1. 信号增强（剪裁负向值并平方，突出高振幅收缩期主波）
        val squared = DoubleArray(signal.size)
        var sum = 0.0
        for (i in signal.indices) {
            val v = signal[i].coerceAtLeast(0.0)
            val sq = v * v
            squared[i] = sq
            sum += sq
        }
        val meanSquared = sum / signal.size

        // 2. 确定快速窗口 W1 (~120ms) 与慢速基线窗口 W2 (~670ms)
        val w1 = (0.12 * sampleRateHz).roundToInt().coerceAtLeast(1)
        val w2 = (0.67 * sampleRateHz).roundToInt().coerceAtLeast(w1 + 1)

        val ma1 = movingAverage(squared, w1)
        val ma2 = movingAverage(squared, w2)

        // 3. 动态自适应阈值：Threshold = MA2 + beta * Mean
        val beta = 0.02
        val thresholdOffset = beta * meanSquared

        // 4. 寻找兴趣区间（Blocks of Interest: MA1 > MA2 + Offset）
        val peaks = mutableListOf<Int>()
        var inBlock = false
        var blockStart = 0

        for (i in signal.indices) {
            val isAbove = ma1[i] > (ma2[i] + thresholdOffset)
            if (isAbove && !inBlock) {
                inBlock = true
                blockStart = i
            } else if (!isAbove && inBlock) {
                inBlock = false
                val blockEnd = i
                if (blockEnd - blockStart >= (w1 / 2).coerceAtLeast(1)) {
                    val peakIdx = findLocalMaxIndex(signal, blockStart, blockEnd)
                    if (peakIdx != null) {
                        peaks.add(peakIdx)
                    }
                }
            }
        }

        if (inBlock && signal.size - blockStart >= (w1 / 2).coerceAtLeast(1)) {
            val peakIdx = findLocalMaxIndex(signal, blockStart, signal.size)
            if (peakIdx != null) {
                peaks.add(peakIdx)
            }
        }

        // 5. 后处理：去除过近的假峰（两峰间距必须大于最小心跳间隔，例如 200BPM 对应 300ms）
        val minIntervalSamples = (0.35 * sampleRateHz).roundToInt().coerceAtLeast(2)
        val filteredPeaks = mutableListOf<Int>()
        for (p in peaks) {
            if (filteredPeaks.isEmpty()) {
                filteredPeaks.add(p)
            } else {
                val last = filteredPeaks.last()
                if (p - last < minIntervalSamples) {
                    if (signal[p] > signal[last]) {
                        filteredPeaks[filteredPeaks.lastIndex] = p
                    }
                } else {
                    filteredPeaks.add(p)
                }
            }
        }

        return filteredPeaks
    }

    private fun movingAverage(data: DoubleArray, windowSize: Int): DoubleArray {
        val n = data.size
        val prefix = DoubleArray(n + 1)
        for (i in 0 until n) {
            prefix[i + 1] = prefix[i] + data[i]
        }
        val out = DoubleArray(n)
        val halfW = windowSize / 2
        for (i in 0 until n) {
            val start = (i - halfW).coerceAtLeast(0)
            val end = (i + halfW + 1).coerceAtMost(n)
            out[i] = (prefix[end] - prefix[start]) / (end - start)
        }
        return out
    }

    private fun findLocalMaxIndex(signal: DoubleArray, start: Int, end: Int): Int? {
        if (start >= end) return null
        var maxVal = Double.NEGATIVE_INFINITY
        var maxIdx = start
        for (i in start until end.coerceAtMost(signal.size)) {
            if (signal[i] > maxVal) {
                maxVal = signal[i]
                maxIdx = i
            }
        }
        return maxIdx
    }
}
