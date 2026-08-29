package com.boompala.engine.bone

import com.nlf.calendar.Solar
import java.time.LocalDate

/**
 * Pure Kotlin engine for Yuan Tiangang Bone Weight Astrology (袁天罡称骨算命).
 * Calculates bone weights based on lunar year, month, day, and earthly branch hour.
 */
object BoneWeightEngine {

    // 60 JiaZi year weights (in tenths of Liang, e.g. 12 = 1.2两)
    private val YEAR_WEIGHTS = mapOf(
        "甲子" to 12, "乙丑" to 9, "丙寅" to 6, "丁卯" to 7, "戊辰" to 12,
        "己巳" to 5, "庚午" to 9, "辛未" to 8, "壬申" to 7, "癸酉" to 8,
        "甲戌" to 15, "乙亥" to 9, "丙子" to 16, "丁丑" to 8, "戊寅" to 8,
        "己卯" to 19, "庚辰" to 12, "辛巳" to 6, "壬午" to 8, "癸未" to 7,
        "甲申" to 5, "乙酉" to 15, "丙戌" to 6, "丁亥" to 16, "戊子" to 15,
        "己丑" to 7, "庚寅" to 9, "辛卯" to 12, "壬辰" to 10, "癸巳" to 7,
        "甲午" to 15, "乙未" to 6, "丙申" to 5, "丁酉" to 14, "戊戌" to 14,
        "己亥" to 9, "庚子" to 7, "辛丑" to 7, "壬寅" to 9, "癸卯" to 12,
        "甲辰" to 8, "乙巳" to 7, "丙午" to 13, "丁未" to 5, "戊申" to 14,
        "己酉" to 5, "庚戌" to 9, "辛亥" to 17, "壬子" to 5, "癸丑" to 7,
        "甲寅" to 12, "乙卯" to 8, "丙辰" to 8, "丁巳" to 16, "戊午" to 19,
        "己未" to 6, "庚申" to 8, "辛酉" to 16, "壬戌" to 10, "癸亥" to 7
    )

    // Lunar month weights (1..12)
    private val MONTH_WEIGHTS = intArrayOf(
        0, 6, 7, 18, 9, 5, 16, 9, 15, 18, 8, 9, 5
    )

    // Lunar day weights (1..30)
    private val DAY_WEIGHTS = intArrayOf(
        0,
        5, 10, 8, 15, 16, 15, 8, 16, 8, 16,
        9, 17, 8, 17, 10, 8, 9, 18, 5, 15,
        10, 9, 8, 9, 15, 18, 7, 8, 16, 6
    )

    // Hour branch weights (0..11 corresponding to 子 丑 寅 卯 辰 巳 午 未 申 酉 戌 亥)
    private val HOUR_WEIGHTS = intArrayOf(
        16, 6, 7, 10, 9, 16, 10, 8, 8, 9, 11, 6
    )

    private fun hourToBranchIndex(hour: Int): Int {
        return when (hour) {
            23, 0 -> 0 // 子
            1, 2 -> 1  // 丑
            3, 4 -> 2  // 寅
            5, 6 -> 3  // 卯
            7, 8 -> 4  // 辰
            9, 10 -> 5 // 巳
            11, 12 -> 6// 午
            13, 14 -> 7// 未
            15, 16 -> 8// 申
            17, 18 -> 9// 酉
            19, 20 -> 10// 戌
            21, 22 -> 11// 亥
            else -> 6
        }
    }

    fun calculate(birthDate: LocalDate, birthHour: Int?): BoneWeightReading {
        val sampleHour = birthHour ?: 12
        val solar = Solar.fromYmdHms(
            birthDate.year,
            birthDate.monthValue,
            birthDate.dayOfMonth,
            sampleHour,
            0,
            0,
        )
        val lunar = solar.lunar
        val yearGanzhi = lunar.yearInGanZhiExact

        val yWeight = YEAR_WEIGHTS[yearGanzhi] ?: 10
        val mWeight = MONTH_WEIGHTS.getOrElse(lunar.month.coerceIn(1, 12)) { 8 }
        val dWeight = DAY_WEIGHTS.getOrElse(lunar.day.coerceIn(1, 30)) { 10 }
        val hWeight = if (birthHour != null) {
            HOUR_WEIGHTS[hourToBranchIndex(birthHour)]
        } else {
            10 // 默认午时平权
        }

        val totalQian = (yWeight + mWeight + dWeight + hWeight).coerceIn(21, 72)
        val (poem, explanation) = getPoemAndExplanation(totalQian)

        return BoneWeightReading(
            birthDate = birthDate,
            birthHour = birthHour,
            lunarDateText = "农历 ${lunar.yearInChinese}年 ${lunar.monthInChinese}月${lunar.dayInChinese}",
            yearGanzhi = yearGanzhi,
            yearWeightQian = yWeight,
            monthWeightQian = mWeight,
            dayWeightQian = dWeight,
            hourWeightQian = hWeight,
            totalWeightQian = totalQian,
            poemLines = poem,
            explanationZh = explanation,
        )
    }

    private fun getPoemAndExplanation(weightQian: Int): Pair<List<String>, String> {
        val poemMap = mapOf(
            21 to Pair(listOf("身寒骨冷苦伶仃", "此命推来行乞人", "碌碌浮生何所倚", "寒门落叶伴晨昏"), "短命非业谓大空，平生灾难也重重。宜积德行善，修心养性。"),
            22 to Pair(listOf("身微骨轻气难平", "朝夕奔波受苦辛", "祖业无凭家计薄", "飘零天涯作羁人"), "身微骨轻，求谋难遂。宜学得一技之长，踏实立身。"),
            23 to Pair(listOf("此命推来骨格轻", "求谋做事事难成", "妻儿兄弟应难许", "独自飘零度过生"), "福薄多舛，六亲冷落。宜安分守己，行善积福。"),
            24 to Pair(listOf("此命推来福禄无", "门庭困苦总难舒", "六亲骨肉皆无靠", "劳碌奔波到白头"), "平生辛苦，劳碌奔波。晚景若能守成，方得安稳。"),
            25 to Pair(listOf("命格推来祖业微", "门庭冷落少光辉", "一生自食其力好", "免得他时受苦悲"), "祖业凋零，全凭自立。自食其力，方免忧愁。"),
            26 to Pair(listOf("平生衣食苦中求", "独自营谋事不休", "离祖出家方得计", "免得劳心作马牛"), "离祖成家，出外谋生为佳。中年渐入佳境。"),
            27 to Pair(listOf("一生做事少商量", "难靠祖宗作主张", "独马单枪空做贼", "平生衣禄亦寻常"), "自谋自立，独当一面。晚运方得小康。"),
            28 to Pair(listOf("一生行事似飘蓬", "祖宗产业在梦中", "若不过房改名姓", "也当移徒二三通"), "四海飘零，多有迁徙。宜外出谋发展。"),
            29 to Pair(listOf("初年运限未曾通", "纵有功名在后冲", "须过四旬方可好", "移居改姓始为荣"), "早年磨砺多阻，四十之后转运亨通。"),
            30 to Pair(listOf("劳劳碌碌苦中求", "东奔西走何日休", "若使终身勤俭过", "老来稍可免忧愁"), "勤俭持家，奔波劳苦。晚年衣食渐丰。"),
            31 to Pair(listOf("忙忙碌碌苦中求", "何日云开见日头", "难得祖基家可立", "中年衣食渐无忧"), "先苦后甜之命。中年家业渐成，无大忧患。"),
            32 to Pair(listOf("初年运蹇事难谋", "渐到中年渐不愁", "兄弟六亲皆得力", "老年福寿两双全"), "早年艰难，中年顺遂，六亲有靠，老运亨通。"),
            33 to Pair(listOf("早年做事事难成", "百计徒劳枉费神", "半世自来衣食薄", "中年富贵始能荣"), "大器晚成。前半生多劳，中晚年富贵自来。"),
            34 to Pair(listOf("此命福气果如何", "僧道门中衣禄多", "离祖出家方得计", "晚年福寿乐呵呵"), "为人清高，心性慈和。自力更生，晚年安泰。"),
            35 to Pair(listOf("平生福量不周全", "祖业根基亦少传", "营谋自立成家计", "到老荣华在晚年"), "白手起家，自立门户。晚景荣华富贵。"),
            36 to Pair(listOf("不须劳碌过平生", "独自成家福不轻", "早岁运来多福泽", "中年衣食自丰盈"), "福泽深厚，自成家业。一生安稳，衣食无忧。"),
            37 to Pair(listOf("此命般般事事能", "劳劳碌碌度平生", "兄弟六亲皆无靠", "自成家计始亨通"), "聪明能干，多才多艺。自立自强，晚年享福。"),
            38 to Pair(listOf("一身骨肉最清高", "早入黉门姓名标", "待到年过三十六", "蓝袍换做紫罗袍"), "少年得志，学业功名早显。三十六岁后大展宏图。"),
            39 to Pair(listOf("此命终身运不通", "劳劳作事尽皆空", "苦心竭力成家计", "到得那时在梦中"), "少年多磨练，中年运渐通。宜稳扎稳打，切莫贪功。"),
            40 to Pair(listOf("平生衣禄是绵长", "件件心中自主张", "前面风霜多受过", "后来必定享安康"), "心有主见，历经风霜。后半生安康享福。"),
            41 to Pair(listOf("此命推来事不同", "为人能干异寻常", "中限交来方称意", "荣华富贵在其中"), "天赋异禀，能力出众。中年交运，富贵双全。"),
            42 to Pair(listOf("得宽怀处且宽怀", "何用双眉皱不开", "若使中年命运济", "那时名利一齐来"), "心宽福自来。中年运转乾坤，名利双收。"),
            43 to Pair(listOf("君尔聪明智谋高", "一生衣禄自难消", "更兼福寿双全美", "富贵荣华乐逍遥"), "聪慧过人，智谋高远。福寿绵长，逍遥富贵。"),
            44 to Pair(listOf("万事由天莫强求", "须知福禄赖前修", "当年财帛难如意", "晚景欣然便不忧"), "随缘而安，顺天应命。晚年财帛丰盈，无忧无虑。"),
            45 to Pair(listOf("名利推来竟若何", "前番辛苦后奔波", "命中注定成家业", "老岁衣食胜从前"), "先苦后甜，天道酬勤。晚年家业丰隆，福泽延绵。"),
            46 to Pair(listOf("东西南北尽皆通", "出姓移居更觉隆", "衣禄无穷无数算", "晚年家道日蒸腾"), "四方逢源，四海通达。晚年家道昌盛，富贵荣华。"),
            47 to Pair(listOf("此命推来旺末年", "妻荣子贵自怡然", "平生衣禄丰盈足", "财富荣华代代传"), "晚运极佳，妻贤子孝。财富丰厚，世代相传。"),
            48 to Pair(listOf("幼年运滞未曾舒", "初限交来渐渐苏", "自立门庭添秀气", "晚年衣食乐丰余"), "少年受抑，后运勃发。自立门庭，晚年富足安乐。"),
            49 to Pair(listOf("此命推来福不轻", "自成自立显门庭", "从来富贵人钦敬", "使婢差奴过一生"), "福气深厚，自成一家。受人敬重，一生富足尊荣。"),
            50 to Pair(listOf("为名为利终日劳", "中年福禄自然高", "老来更有新景象", "富贵荣华第一豪"), "中年得运，福禄崇高。晚年更有新气象，富贵荣华。"),
            51 to Pair(listOf("一世荣华事事通", "不须劳碌自然丰", "弟兄叔侄皆如意", "家业丰隆百世宏"), "事事通达，福禄天成。六亲和睦，家业繁盛。"),
            52 to Pair(listOf("一世亨通事事宜", "门庭光彩有人知", "一生福禄自然来", "富贵荣华世所稀"), "门庭生辉，名显四海。福禄不求自至，富贵稀有。"),
            53 to Pair(listOf("此格推来气象真", "兴家立业在其中", "一生福禄安排定", "富贵荣华显祖宗"), "大富大贵，光耀门楣。前程远大，福禄天定。"),
            54 to Pair(listOf("此命推来厚且坚", "平生衣禄自然全", "前程广阔多通达", "富贵荣华在晚年"), "根基稳固，福禄双全。前程通达无碍，安享晚福。"),
            55 to Pair(listOf("走马扬鞭争利名", "少年作事费筹营", "一朝福禄乘时至", "富贵荣华震远名"), "早年奋进，中年乘风而起。名扬四海，富贵显达。"),
            56 to Pair(listOf("礼义相逢自古称", "平生富贵姓名馨", "若蒙帝宠兼天福", "金玉满堂耀客庭"), "德才兼备，声名远扬。福泽绵延，金玉满堂。"),
            57 to Pair(listOf("福禄丰盈万事全", "一生荣华乐天然", "朝中卿相兼知友", "富贵荣华代代传"), "福禄盈门，天乐自得。交游显贵，代代相传。"),
            58 to Pair(listOf("平生福禄自然来", "名利兼全福寿偕", "雁塔题名登贵客", "紫袍金带耀门台"), "名利兼收，福寿延绵。功名早成，荣华显贵。"),
            59 to Pair(listOf("细推此格妙且清", "必定才高学业成", "甲第及第人钦仰", "富贵双全在帝京"), "才华横溢，学贯古今。受人仰慕，富贵双全。"),
            60 to Pair(listOf("一朝金榜快题名", "显祖荣宗立大勋", "衣食自然丰足足", "田园产业耀前庭"), "金榜题名，光宗耀祖。家业殷实，福禄齐天。"),
            61 to Pair(listOf("不须劳碌过平生", "百万家资自称心", "朝中大臣皆接引", "富贵双全步青云"), "贵人扶持，平步青云。家产丰隆，富贵自足。"),
            62 to Pair(listOf("此命生来福自高", "天生富贵逞英豪", "朝中做官名声远", "四海声明播九霄"), "生来福大，豪气干云。名震朝野，威震九霄。"),
            63 to Pair(listOf("命主为官福禄长", "得来富贵受皇恩", "名题金塔传芳誉", "世代荣昌耀子孙"), "为官做宰，皇恩浩荡。名扬青史，子孙昌盛。"),
            64 to Pair(listOf("此格推来礼义通", "一身福禄用无穷", "甜酸苦辣皆尝过", "富贵荣华稳且丰"), "胸怀大度，福禄无尽。饱经世事，富贵极其安稳。"),
            65 to Pair(listOf("细推此命福非轻", "富贵荣华及弟兄", "盛世文章耀天府", "功名显赫冠群英"), "功名显赫，冠绝群伦。家族沾光，福泽深厚。"),
            66 to Pair(listOf("此格人间一等福", "荣华富贵享安康", "一生常作太平客", "万卷诗书在腹中"), "人间一等之福。富贵安康，文韬武略，一生太平。"),
            67 to Pair(listOf("此命推来福自丰", "高官厚禄在朝中", "富贵荣华耀门第", "金玉满堂寿如松"), "位极人臣，高官厚禄。福寿齐天，长乐无极。"),
            68 to Pair(listOf("富贵由天莫强求", "万事无忧乐悠悠", "黄金白银堆满屋", "福禄双全老来福"), "富甲天下，无忧无虑。金银满堂，长寿多福。"),
            69 to Pair(listOf("君是人间福禄星", "一生富贵自然荣", "紫袍玉带为卿相", "声震朝纲万国称"), "人间福星，位极卿相。声震朝野，万国来朝。"),
            70 to Pair(listOf("此命推来福不轻", "不须愁虑苦劳心", "一身富贵天然定", "寿算延绵享太平"), "天命富贵，毋须劳心。寿比南山，太平安乐。"),
            71 to Pair(listOf("此命生来大不同", "公侯卿相在其中", "一生富贵荣华极", "福寿双全享百龄"), "公侯将相之格。极品富贵，福寿双全，享寿期颐。"),
            72 to Pair(listOf("此格推来礼义通", "一身福禄用无穷", "甜酸苦辣皆尝过", "滚滚财源稳且丰"), "罕世奇格。至尊福泽，财源滚滚，万事圆满。")
        )
        return poemMap[weightQian] ?: Pair(
            listOf("此命推来福禄全", "平生衣食乐安然", "积德行善修心性", "富贵荣华代代传"),
            "命格祥和，福泽自来。踏实守成，福寿绵延。"
        )
    }
}
