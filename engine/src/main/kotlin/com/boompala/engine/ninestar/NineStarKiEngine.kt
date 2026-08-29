package com.boompala.engine.ninestar

import com.boompala.engine.model.FiveElement
import com.nlf.calendar.NineStar
import com.nlf.calendar.Solar
import java.time.LocalDate

/**
 * Pure Kotlin engine for Nine Star Ki astrology (九星气学).
 * Backed by 6tail/lunar-java NineStar calendar data.
 */
object NineStarKiEngine {

    private val STAR_DEFINITIONS = mapOf(
        1 to NineStarProfile(
            index = 1,
            nameZh = "一白贪狼水星",
            trigramZh = "坎水",
            element = FiveElement.WATER,
            colorZh = "白 / 深蓝",
            natureZh = "润下灵动 · 随方就圆",
            luckyDirectionsZh = "正北、正东、东南",
            personalityZh = "心性柔韧如水，洞察敏锐，善于适应变化。富有耐力与社交亲和力，喜深思与探索潜意识智慧。",
        ),
        2 to NineStarProfile(
            index = 2,
            nameZh = "二黑巨门土星",
            trigramZh = "坤土",
            element = FiveElement.EARTH,
            colorZh = "黑 / 棕褐",
            natureZh = "厚德载物 · 滋养护持",
            luckyDirectionsZh = "西南、正西、西北",
            personalityZh = "性格敦厚务实，任劳任怨，极具奉献精神与包容心。重视细节与安全感，是坚实的幕后支柱。",
        ),
        3 to NineStarProfile(
            index = 3,
            nameZh = "三碧禄存木星",
            trigramZh = "震木",
            element = FiveElement.WOOD,
            colorZh = "碧绿",
            natureZh = "雷动生发 · 勇往直前",
            luckyDirectionsZh = "正东、正南、东南",
            personalityZh = "充满生机活力与开拓爆发力，敢为人先，行动迅速。正义感强，言出必行，善于打破陈规。",
        ),
        4 to NineStarProfile(
            index = 4,
            nameZh = "四绿文曲木星",
            trigramZh = "巽木",
            element = FiveElement.WOOD,
            colorZh = "翠绿 / 浅绿",
            natureZh = "风行通达 · 儒雅斯文",
            luckyDirectionsZh = "东南、正东、正南",
            personalityZh = "温文尔雅，思维灵活敏捷，具有极强的社交情商与文学艺术鉴赏力。善于协调关系与融会贯通。",
        ),
        5 to NineStarProfile(
            index = 5,
            nameZh = "五黄廉贞土星",
            trigramZh = "中宫土",
            element = FiveElement.EARTH,
            colorZh = "正黄",
            natureZh = "中正统摄 · 威仪万方",
            luckyDirectionsZh = "东北、西南、中宫",
            personalityZh = "天生具备强大的意志力与统率气场，不怒自威。耐受力极强，能担重任，具有颠覆与重建之大能。",
        ),
        6 to NineStarProfile(
            index = 6,
            nameZh = "六白武曲金星",
            trigramZh = "乾金",
            element = FiveElement.METAL,
            colorZh = "乳白 / 金",
            natureZh = "自强刚健 · 尊贵自律",
            luckyDirectionsZh = "西北、正西、东北",
            personalityZh = "刚毅果决，自律自强，追求卓越与完美。讲究原则与忠诚度，具天然领袖风范与管理才能。",
        ),
        7 to NineStarProfile(
            index = 7,
            nameZh = "七赤破军金星",
            trigramZh = "兑金",
            element = FiveElement.METAL,
            colorZh = "赤红 / 银白",
            natureZh = "悦动明快 · 口才通神",
            luckyDirectionsZh = "正西、西北、西南",
            personalityZh = "性情爽朗开朗，能言善辩，擅长交际与营造欢乐氛围。感知敏锐，对美感、美食与物质生活充满热情。",
        ),
        8 to NineStarProfile(
            index = 8,
            nameZh = "八白左辅土星",
            trigramZh = "艮土",
            element = FiveElement.EARTH,
            colorZh = "白 / 浅褐",
            natureZh = "止观不动 · 厚重深沉",
            luckyDirectionsZh = "东北、西北、西南",
            personalityZh = "沉静内敛，踏实稳健如高山峻岭。做事步步为营，极富毅力与坚守力，守得住繁华，耐得住寂寞。",
        ),
        9 to NineStarProfile(
            index = 9,
            nameZh = "九紫右弼火星",
            trigramZh = "离火",
            element = FiveElement.FIRE,
            colorZh = "紫红 / 艳红",
            natureZh = "光明璀璨 · 热情奔放",
            luckyDirectionsZh = "正南、正东、东南",
            personalityZh = "才情焕发，热情明朗，极具魅力与号召力。追求精神与光明的高洁之士，富有灵感悟性与远见卓识。",
        ),
    )

    fun calculate(birthDate: LocalDate): NineStarKiReading {
        val solar = Solar.fromYmdHms(
            birthDate.year,
            birthDate.monthValue,
            birthDate.dayOfMonth,
            12,
            0,
            0,
        )
        val lunar = solar.lunar
        val yearStarRaw = lunar.yearNineStar
        val monthStarRaw = lunar.monthNineStar

        val yearIndex = parseStarIndex(yearStarRaw)
        val monthIndex = parseStarIndex(monthStarRaw)

        val yearProfile = STAR_DEFINITIONS[yearIndex] ?: STAR_DEFINITIONS.getValue(1)
        val monthProfile = STAR_DEFINITIONS[monthIndex] ?: STAR_DEFINITIONS.getValue(1)

        val prevJie = lunar.prevJieQi
        val nextJie = lunar.nextJieQi
        val solarTermText = "${prevJie.name}中节"

        val theme = "${yearProfile.nameZh}坐命 · 外显${yearProfile.element.displayName}气，内蕴${monthProfile.element.displayName}局"

        return NineStarKiReading(
            birthDate = birthDate,
            yearStar = yearProfile,
            monthStar = monthProfile,
            solarTermText = solarTermText,
            energyThemeZh = theme,
        )
    }

    private fun parseStarIndex(nineStar: NineStar): Int {
        val numStr = nineStar.number
        return when (numStr) {
            "一" -> 1
            "二" -> 2
            "三" -> 3
            "四" -> 4
            "五" -> 5
            "六" -> 6
            "七" -> 7
            "八" -> 8
            "九" -> 9
            else -> 1
        }
    }
}
