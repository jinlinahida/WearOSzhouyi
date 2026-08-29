package com.boompala.engine.astrology

import java.time.LocalDate

/**
 * The four classical astrological elements (四象).
 */
enum class ZodiacElement(
    val displayNameZh: String,
    val displayNameEn: String,
    val keywordsZh: String,
    val colorHex: Long,
) {
    FIRE("火象", "Fire", "热情 · 进取 · 活力", 0xFFFF7043),
    EARTH("土象", "Earth", "务实 · 稳健 · 秩序", 0xFFFFCA28),
    AIR("风象", "Air", "思辨 · 敏锐 · 沟通", 0xFF42A5F5),
    WATER("水象", "Water", "直觉 · 情感 · 共情", 0xFF26C6DA);
}

/**
 * Astrological modalities (三方态).
 */
enum class ZodiacModality(
    val displayNameZh: String,
    val displayNameEn: String,
) {
    CARDINAL("基本宫", "Cardinal"),
    FIXED("固定宫", "Fixed"),
    MUTABLE("变动宫", "Mutable");
}

/**
 * Astrological polarity (阴阳极性).
 */
enum class ZodiacPolarity(
    val displayNameZh: String,
    val displayNameEn: String,
) {
    YANG("阳性", "Yang"),
    YIN("阴性", "Yin");
}

/**
 * The 12 signs of the Tropical Zodiac (黄道十二星座).
 */
enum class ZodiacSign(
    val displayNameZh: String,
    val displayNameEn: String,
    val symbol: String,
    val element: ZodiacElement,
    val modality: ZodiacModality,
    val polarity: ZodiacPolarity,
    val rulerZh: String,
    val dateRangeZh: String,
) {
    ARIES("白羊座", "Aries", "♈", ZodiacElement.FIRE, ZodiacModality.CARDINAL, ZodiacPolarity.YANG, "火星", "03.21 - 04.19"),
    TAURUS("金牛座", "Taurus", "♉", ZodiacElement.EARTH, ZodiacModality.FIXED, ZodiacPolarity.YIN, "金星", "04.20 - 05.20"),
    GEMINI("双子座", "Gemini", "♊", ZodiacElement.AIR, ZodiacModality.MUTABLE, ZodiacPolarity.YANG, "水星", "05.21 - 06.21"),
    CANCER("巨蟹座", "Cancer", "♋", ZodiacElement.WATER, ZodiacModality.CARDINAL, ZodiacPolarity.YIN, "月亮", "06.22 - 07.22"),
    LEO("狮子座", "Leo", "♌", ZodiacElement.FIRE, ZodiacModality.FIXED, ZodiacPolarity.YANG, "太阳", "07.23 - 08.22"),
    VIRGO("处女座", "Virgo", "♍", ZodiacElement.EARTH, ZodiacModality.MUTABLE, ZodiacPolarity.YIN, "水星", "08.23 - 09.22"),
    LIBRA("天秤座", "Libra", "♎", ZodiacElement.AIR, ZodiacModality.CARDINAL, ZodiacPolarity.YANG, "金星", "09.23 - 10.23"),
    SCORPIO("天蝎座", "Scorpio", "♏", ZodiacElement.WATER, ZodiacModality.FIXED, ZodiacPolarity.YIN, "冥王/火星", "10.24 - 11.22"),
    SAGITTARIUS("射手座", "Sagittarius", "♐", ZodiacElement.FIRE, ZodiacModality.MUTABLE, ZodiacPolarity.YANG, "木星", "11.23 - 12.21"),
    CAPRICORN("摩羯座", "Capricorn", "♑", ZodiacElement.EARTH, ZodiacModality.CARDINAL, ZodiacPolarity.YIN, "土星", "12.22 - 01.19"),
    AQUARIUS("水瓶座", "Aquarius", "♒", ZodiacElement.AIR, ZodiacModality.FIXED, ZodiacPolarity.YANG, "天王/土星", "01.20 - 02.18"),
    PISCES("双鱼座", "Pisces", "♓", ZodiacElement.WATER, ZodiacModality.MUTABLE, ZodiacPolarity.YIN, "海王/木星", "02.19 - 03.20");

    companion object {
        fun fromEclipticLongitude(longitudeDeg: Double): Pair<ZodiacSign, Double> {
            var normalized = longitudeDeg % 360.0
            if (normalized < 0) normalized += 360.0
            val index = (normalized / 30.0).toInt().coerceIn(0, 11)
            val degreeInSign = normalized - (index * 30.0)
            return entries[index] to degreeInSign
        }
    }
}

/**
 * Astrological celestial bodies & sensitive points.
 */
enum class CelestialBody(
    val displayNameZh: String,
    val displayNameEn: String,
    val symbol: String,
    val roleZh: String,
) {
    SUN("太阳", "Sun", "☉", "核心自我 · 人格意志"),
    MOON("月亮", "Moon", "☽", "内在情绪 · 潜意识归属"),
    ASCENDANT("上升", "Ascendant", "ASC", "外在面具 · 第一印象"),
    MERCURY("水星", "Mercury", "☿", "思维认知 · 表达沟通"),
    VENUS("金星", "Venus", "♀", "审美喜好 · 情感人际"),
    MARS("火星", "Mars", "♂", "行动爆发 · 意志驱动"),
    JUPITER("木星", "Jupiter", "♃", "宏观视野 · 幸运拓展"),
    SATURN("土星", "Saturn", "♄", "结构纪律 · 现实考验");
}

/**
 * A placement of a celestial body in the chart.
 */
data class PlanetPlacement(
    val body: CelestialBody,
    val sign: ZodiacSign,
    val degreeInSign: Double,
    val houseNumber: Int = 1,
) {
    val formattedDegree: String
        get() {
            val deg = degreeInSign.toInt()
            val min = ((degreeInSign - deg) * 60.0).toInt().coerceIn(0, 59)
            return "${deg}°${min.toString().padStart(2, '0')}'"
        }

    val displaySummaryZh: String
        get() = "${body.displayNameZh}落${sign.displayNameZh} · 第${houseNumber}宫 (${formattedDegree})"
}

/**
 * Statistical balance of the Four Elements (火土风水).
 */
data class ElementBalance(
    val fireCount: Int,
    val earthCount: Int,
    val airCount: Int,
    val waterCount: Int,
) {
    val totalCount: Int get() = fireCount + earthCount + airCount + waterCount

    fun percentage(element: ZodiacElement): Int {
        if (totalCount == 0) return 25
        val count = when (element) {
            ZodiacElement.FIRE -> fireCount
            ZodiacElement.EARTH -> earthCount
            ZodiacElement.AIR -> airCount
            ZodiacElement.WATER -> waterCount
        }
        return ((count.toDouble() / totalCount) * 100).toInt()
    }

    val dominantElement: ZodiacElement
        get() {
            val list = listOf(
                ZodiacElement.FIRE to fireCount,
                ZodiacElement.EARTH to earthCount,
                ZodiacElement.AIR to airCount,
                ZodiacElement.WATER to waterCount,
            )
            return list.maxByOrNull { it.second }?.first ?: ZodiacElement.FIRE
        }

    val balanceSummaryZh: String
        get() = when (dominantElement) {
            ZodiacElement.FIRE -> "火象充沛 · 行动力与进取心强"
            ZodiacElement.EARTH -> "土象主导 · 沉稳务实且注重秩序"
            ZodiacElement.AIR -> "风象突出 · 思维活跃且善于变通"
            ZodiacElement.WATER -> "水象深厚 · 直觉敏锐且共情力强"
        }
}

/**
 * Complete Western Natal Chart Reading.
 */
data class WesternChartReading(
    val birthDate: LocalDate,
    val birthHour: Int?,
    val sun: PlanetPlacement,
    val moon: PlanetPlacement,
    val ascendant: PlanetPlacement?,
    val planets: List<PlanetPlacement>,
    val elementBalance: ElementBalance,
) {
    val bigThreeSummary: String
        get() = buildString {
            append("日${sun.sign.displayNameZh.removeSuffix("座")}")
            append(" · 月${moon.sign.displayNameZh.removeSuffix("座")}")
            if (ascendant != null) {
                append(" · 升${ascendant.sign.displayNameZh.removeSuffix("座")}")
            }
        }
}
