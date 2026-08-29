package com.boompala.engine.numerology

import java.time.LocalDate

/**
 * Pure Kotlin engine for Pythagorean Numerology.
 */
object NumerologyEngine {

    private val LIFE_PATH_ARCHETYPES = mapOf(
        1 to LifePathInfo(1, "领袖开拓者", "The Leader", "独立 · 勇气 · 原创性", "具有与生俱来的前瞻视角与开拓勇气，善于独立开辟新天地，克服困难，引领方向。"),
        2 to LifePathInfo(2, "和谐协调者", "The Peacemaker", "合作 · 敏锐 · 疗愈力", "天生具备细致的同理心与沟通天赋，追求平和与共赢，是卓越的倾听者与合作桥梁。"),
        3 to LifePathInfo(3, "灵感表达者", "The Creative", "热情 · 创意 · 表达力", "思维跳跃且充满艺术感染力，乐观幽默，善于用言语、文字或艺术点亮周围人的生活。"),
        4 to LifePathInfo(4, "务实建设者", "The Builder", "踏实 · 秩序 · 责任心", "注重细节与组织架构，耐心坚韧，善于将宏大蓝图转化为扎实落地的每一步行动。"),
        5 to LifePathInfo(5, "自由探索者", "The Adventurer", "应变 · 探索 · 多面手", "热爱未知与多变，求知欲旺盛，敢于突破既有框架，能在变化莫测的环境中快速适应。"),
        6 to LifePathInfo(6, "温情奉献者", "The Nurturer", "关怀 · 责任 · 美感爱", "具有极强的家庭观与责任感，乐于为集体与他人遮风挡雨，创造温暖温馨的平衡环境。"),
        7 to LifePathInfo(7, "真理哲思者", "The Seeker", "洞见 · 深度 · 独处思", "善于透过表象探寻底层逻辑与精神奥秘，分析力与直觉极佳，在深度钻研中收获智慧。"),
        8 to LifePathInfo(8, "丰盛掌控者", "The Achiever", "宏观 · 魄力 · 统筹力", "具有宏大的现实远见与商业魄力，善于调配资源与驾驭复杂局面，达成物质与精神的双重丰盛。"),
        9 to LifePathInfo(9, "博爱人道者", "The Humanitarian", "包容 · 慈悲 · 理想家", "心怀广阔世界，同情弱者，具有超越小我的博爱胸怀与无私奉献的精神高度。"),
        11 to LifePathInfo(11, "灵性启迪大师", "Master Illuminator", "直觉 · 启蒙 · 愿景光", "卓越数 11 具备极高直觉敏锐度与灵性顿悟力，如同灯塔般照亮他人，激发团队潜能。", isMasterNumber = true),
        22 to LifePathInfo(22, "宏图建造大师", "Master Builder", "远见 · 缔造 · 宏伟业", "卓越数 22 结合了 4 的踏实与 11 的高维远见，善于将崇高理想缔造成惠及大众的宏伟工程。", isMasterNumber = true),
        33 to LifePathInfo(33, "大爱疗愈大师", "Master Teacher", "奉献 · 纯粹 · 治愈心", "卓越数 33 是大爱与慈悲的极致体现，以无条件的关爱与高尚风范治愈人心，启蒙众生。", isMasterNumber = true),
    )

    fun calculate(birthDate: LocalDate, currentYear: Int = LocalDate.now().year): NumerologyReading {
        val y = reduceDigits(birthDate.year)
        val m = reduceDigits(birthDate.monthValue)
        val d = reduceDigits(birthDate.dayOfMonth)

        val lifePath = reduceDigits(y + m + d)
        val info = LIFE_PATH_ARCHETYPES[lifePath] ?: LIFE_PATH_ARCHETYPES.getValue(reduceToSingleDigit(lifePath))

        val birthdayNumber = reduceToSingleDigit(birthDate.dayOfMonth)
        val attitudeNumber = reduceToSingleDigit(birthDate.monthValue + birthDate.dayOfMonth)
        val personalYearNumber = reduceToSingleDigit(reduceDigits(currentYear) + birthDate.monthValue + birthDate.dayOfMonth)

        val digitString = "${birthDate.year}${birthDate.monthValue.toString().padStart(2, '0')}${birthDate.dayOfMonth.toString().padStart(2, '0')}"
        val counts = mutableMapOf<Int, Int>()
        for (ch in digitString) {
            val num = ch.digitToIntOrNull()
            if (num != null && num in 1..9) {
                counts[num] = (counts[num] ?: 0) + 1
            }
        }

        fun hasLine(digits: List<Int>): Boolean = digits.all { (counts[it] ?: 0) > 0 }

        val lines = listOf(
            LoShuLine("思维认知线", listOf(1, 2, 3), hasLine(listOf(1, 2, 3)), "条理清晰，逻辑分析与理解能力强"),
            LoShuLine("意志执行线", listOf(4, 5, 6), hasLine(listOf(4, 5, 6)), "意志坚韧，目标坚定且执行果决"),
            LoShuLine("活力行动线", listOf(7, 8, 9), hasLine(listOf(7, 8, 9)), "充满朝气，勇于实践与开拓新局"),
            LoShuLine("务实物质线", listOf(1, 4, 7), hasLine(listOf(1, 4, 7)), "注重现实与生活秩序，脚踏实地"),
            LoShuLine("情感平衡线", listOf(2, 5, 8), hasLine(listOf(2, 5, 8)), "情绪平稳，同理心强，人际和谐"),
            LoShuLine("智慧灵性线", listOf(3, 6, 9), hasLine(listOf(3, 6, 9)), "想象丰富，具博爱心与文化艺术悟性"),
            LoShuLine("坚定毅力线", listOf(1, 5, 9), hasLine(listOf(1, 5, 9)), "持之以恒，面对阻碍百折不挠"),
            LoShuLine("同理直觉线", listOf(3, 5, 7), hasLine(listOf(3, 5, 7)), "直觉敏锐，善于洞察他人深层心理"),
        )

        return NumerologyReading(
            birthDate = birthDate,
            lifePathNumber = lifePath,
            lifePathInfo = info,
            birthdayNumber = birthdayNumber,
            attitudeNumber = attitudeNumber,
            personalYearNumber = personalYearNumber,
            loShuGrid = LoShuGrid(counts, lines),
        )
    }

    private fun reduceDigits(value: Int): Int {
        var num = value
        while (num > 9 && num !in setOf(11, 22, 33)) {
            var sum = 0
            var tmp = num
            while (tmp > 0) {
                sum += tmp % 10
                tmp /= 10
            }
            num = sum
        }
        return num
    }

    private fun reduceToSingleDigit(value: Int): Int {
        var num = value
        while (num > 9) {
            var sum = 0
            var tmp = num
            while (tmp > 0) {
                sum += tmp % 10
                tmp /= 10
            }
            num = sum
        }
        return num
    }
}
