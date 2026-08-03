package com.boompala.compass

import kotlin.math.abs

data class CompassReading(
    val degrees: Float,
    val eightDirection: String,
    val mountain: Mountain,
    val sittingMountain: Mountain,
    val trigram: Trigram,
)

enum class YinYang(val displayName: String) { YIN("阴"), YANG("阳") }

data class Trigram(
    val name: String,
    val symbol: String,
    val direction: String,
    val element: String,
    val luoShuNumber: Int,
    val centerDegrees: Int,
)

data class Mountain(
    val name: String,
    val centerDegrees: Int,
    val startDegrees: Float,
    val endDegrees: Float,
    val element: String,
    val yinYang: YinYang,
) {
    val rangeLabel: String get() = "${format(startDegrees)}°–${format(endDegrees)}°"
    private fun format(value: Float): String = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
}

/** Pure, magnetic-bearing presentation rules. 0° is magnetic north, clockwise positive. */
object CompassMath {
    private val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    // Centers are the traditional Earth-plate order: 子 starts at 0° and sectors wrap at north.
    val trigrams: List<Trigram> = listOf(
        Trigram("坎", "☵", "北", "水", 1, 0), Trigram("艮", "☶", "东北", "土", 8, 45),
        Trigram("震", "☳", "东", "木", 3, 90), Trigram("巽", "☴", "东南", "木", 4, 135),
        Trigram("离", "☲", "南", "火", 9, 180), Trigram("坤", "☷", "西南", "土", 2, 225),
        Trigram("兑", "☱", "西", "金", 7, 270), Trigram("乾", "☰", "西北", "金", 6, 315),
    )
    val mountains: List<Mountain> = listOf(
        mountain("子", 0, 352.5f, 7.5f, "水", YinYang.YIN), mountain("癸", 15, 7.5f, 22.5f, "水", YinYang.YIN),
        mountain("丑", 30, 22.5f, 37.5f, "土", YinYang.YIN), mountain("艮", 45, 37.5f, 52.5f, "土", YinYang.YANG),
        mountain("寅", 60, 52.5f, 67.5f, "木", YinYang.YANG), mountain("甲", 75, 67.5f, 82.5f, "木", YinYang.YANG),
        mountain("卯", 90, 82.5f, 97.5f, "木", YinYang.YIN), mountain("乙", 105, 97.5f, 112.5f, "木", YinYang.YIN),
        mountain("辰", 120, 112.5f, 127.5f, "土", YinYang.YIN), mountain("巽", 135, 127.5f, 142.5f, "木", YinYang.YANG),
        mountain("巳", 150, 142.5f, 157.5f, "火", YinYang.YANG), mountain("丙", 165, 157.5f, 172.5f, "火", YinYang.YANG),
        mountain("午", 180, 172.5f, 187.5f, "火", YinYang.YIN), mountain("丁", 195, 187.5f, 202.5f, "火", YinYang.YIN),
        mountain("未", 210, 202.5f, 217.5f, "土", YinYang.YIN), mountain("坤", 225, 217.5f, 232.5f, "土", YinYang.YANG),
        mountain("申", 240, 232.5f, 247.5f, "金", YinYang.YANG), mountain("庚", 255, 247.5f, 262.5f, "金", YinYang.YANG),
        mountain("酉", 270, 262.5f, 277.5f, "金", YinYang.YIN), mountain("辛", 285, 277.5f, 292.5f, "金", YinYang.YIN),
        mountain("戌", 300, 292.5f, 307.5f, "土", YinYang.YIN), mountain("乾", 315, 307.5f, 322.5f, "金", YinYang.YANG),
        mountain("亥", 330, 322.5f, 337.5f, "水", YinYang.YANG), mountain("壬", 345, 337.5f, 352.5f, "水", YinYang.YANG),
    )

    private fun mountain(name: String, center: Int, start: Float, end: Float, element: String, yinYang: YinYang) =
        Mountain(name, center, start, end, element, yinYang)

    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    fun reading(degrees: Float): CompassReading {
        val normalized = normalize(degrees)
        val directionIndex = (((normalized + 22.5f) / 45f).toInt()) % 8
        val mountain = mountains.first { if (it.name == "子") normalized >= 352.5f || normalized < 7.5f else normalized >= it.startDegrees && normalized < it.endDegrees }
        val sittingMountain = mountainFor(normalized + 180f)
        val trigram = trigrams[directionIndex]
        return CompassReading(normalized, directions[directionIndex], mountain, sittingMountain, trigram)
    }

    private fun mountainFor(degrees: Float): Mountain {
        val normalized = normalize(degrees)
        return mountains.first { if (it.name == "子") normalized >= 352.5f || normalized < 7.5f else normalized >= it.startDegrees && normalized < it.endDegrees }
    }

    /** Circular shortest-path interpolation; prevents a 359°→0° full-turn jump. */
    fun smooth(previous: Float?, current: Float, factor: Float): Float {
        if (previous == null) return normalize(current)
        val t = factor.coerceIn(0.01f, 1f)
        var delta = normalize(current) - normalize(previous)
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        if (abs(delta) < 0.35f) return normalize(previous)
        return normalize(previous + delta * t)
    }

    fun angularDistance(a: Float, b: Float): Float {
        val d = abs(normalize(a) - normalize(b))
        return minOf(d, 360f - d)
    }
}

/** Platform-free state machine used to verify lock, availability and lifecycle behavior. */
data class CompassSession(
    val liveHeading: Float? = null,
    val lockedHeading: Float? = null,
    val sensorAvailable: Boolean = true,
    val accuracy: Int = 0,
    val listening: Boolean = false,
) {
    val displayedHeading: Float? get() = lockedHeading ?: liveHeading
    val locked: Boolean get() = lockedHeading != null
    fun onSensor(heading: Float, newAccuracy: Int) = copy(liveHeading = CompassMath.normalize(heading), accuracy = newAccuracy)
    fun lock() = if (liveHeading == null) this else copy(lockedHeading = liveHeading)
    fun resumeLive() = copy(lockedHeading = null)
    fun onVisible() = copy(listening = sensorAvailable)
    fun onHidden() = copy(listening = false)
    fun unavailable() = copy(sensorAvailable = false, listening = false, liveHeading = null, lockedHeading = null)
}
