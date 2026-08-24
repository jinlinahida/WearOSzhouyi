package com.boompala.ui

import androidx.annotation.DrawableRes
import com.boompala.R
import com.boompala.engine.tarot.TarotCard

/**
 * Maps Tarot card domain codes to Android drawable resources.
 */
object TarotImageAssets {

    @DrawableRes
    val cardBackResId: Int = R.drawable.tarot_card_back

    @DrawableRes
    fun cardDrawableRes(code: String): Int = when (code) {
        "major_00" -> R.drawable.tarot_card_major_00
        "major_01" -> R.drawable.tarot_card_major_01
        "major_02" -> R.drawable.tarot_card_major_02
        "major_03" -> R.drawable.tarot_card_major_03
        "major_04" -> R.drawable.tarot_card_major_04
        "major_05" -> R.drawable.tarot_card_major_05
        "major_06" -> R.drawable.tarot_card_major_06
        "major_07" -> R.drawable.tarot_card_major_07
        "major_08" -> R.drawable.tarot_card_major_08
        "major_09" -> R.drawable.tarot_card_major_09
        "major_10" -> R.drawable.tarot_card_major_10
        "major_11" -> R.drawable.tarot_card_major_11
        "major_12" -> R.drawable.tarot_card_major_12
        "major_13" -> R.drawable.tarot_card_major_13
        "major_14" -> R.drawable.tarot_card_major_14
        "major_15" -> R.drawable.tarot_card_major_15
        "major_16" -> R.drawable.tarot_card_major_16
        "major_17" -> R.drawable.tarot_card_major_17
        "major_18" -> R.drawable.tarot_card_major_18
        "major_19" -> R.drawable.tarot_card_major_19
        "major_20" -> R.drawable.tarot_card_major_20
        "major_21" -> R.drawable.tarot_card_major_21

        "wands_01" -> R.drawable.tarot_card_wands_01
        "wands_02" -> R.drawable.tarot_card_wands_02
        "wands_03" -> R.drawable.tarot_card_wands_03
        "wands_04" -> R.drawable.tarot_card_wands_04
        "wands_05" -> R.drawable.tarot_card_wands_05
        "wands_06" -> R.drawable.tarot_card_wands_06
        "wands_07" -> R.drawable.tarot_card_wands_07
        "wands_08" -> R.drawable.tarot_card_wands_08
        "wands_09" -> R.drawable.tarot_card_wands_09
        "wands_10" -> R.drawable.tarot_card_wands_10
        "wands_11" -> R.drawable.tarot_card_wands_11
        "wands_12" -> R.drawable.tarot_card_wands_12
        "wands_13" -> R.drawable.tarot_card_wands_13
        "wands_14" -> R.drawable.tarot_card_wands_14

        "cups_01" -> R.drawable.tarot_card_cups_01
        "cups_02" -> R.drawable.tarot_card_cups_02
        "cups_03" -> R.drawable.tarot_card_cups_03
        "cups_04" -> R.drawable.tarot_card_cups_04
        "cups_05" -> R.drawable.tarot_card_cups_05
        "cups_06" -> R.drawable.tarot_card_cups_06
        "cups_07" -> R.drawable.tarot_card_cups_07
        "cups_08" -> R.drawable.tarot_card_cups_08
        "cups_09" -> R.drawable.tarot_card_cups_09
        "cups_10" -> R.drawable.tarot_card_cups_10
        "cups_11" -> R.drawable.tarot_card_cups_11
        "cups_12" -> R.drawable.tarot_card_cups_12
        "cups_13" -> R.drawable.tarot_card_cups_13
        "cups_14" -> R.drawable.tarot_card_cups_14

        "swords_01" -> R.drawable.tarot_card_swords_01
        "swords_02" -> R.drawable.tarot_card_swords_02
        "swords_03" -> R.drawable.tarot_card_swords_03
        "swords_04" -> R.drawable.tarot_card_swords_04
        "swords_05" -> R.drawable.tarot_card_swords_05
        "swords_06" -> R.drawable.tarot_card_swords_06
        "swords_07" -> R.drawable.tarot_card_swords_07
        "swords_08" -> R.drawable.tarot_card_swords_08
        "swords_09" -> R.drawable.tarot_card_swords_09
        "swords_10" -> R.drawable.tarot_card_swords_10
        "swords_11" -> R.drawable.tarot_card_swords_11
        "swords_12" -> R.drawable.tarot_card_swords_12
        "swords_13" -> R.drawable.tarot_card_swords_13
        "swords_14" -> R.drawable.tarot_card_swords_14

        "pentacles_01" -> R.drawable.tarot_card_pentacles_01
        "pentacles_02" -> R.drawable.tarot_card_pentacles_02
        "pentacles_03" -> R.drawable.tarot_card_pentacles_03
        "pentacles_04" -> R.drawable.tarot_card_pentacles_04
        "pentacles_05" -> R.drawable.tarot_card_pentacles_05
        "pentacles_06" -> R.drawable.tarot_card_pentacles_06
        "pentacles_07" -> R.drawable.tarot_card_pentacles_07
        "pentacles_08" -> R.drawable.tarot_card_pentacles_08
        "pentacles_09" -> R.drawable.tarot_card_pentacles_09
        "pentacles_10" -> R.drawable.tarot_card_pentacles_10
        "pentacles_11" -> R.drawable.tarot_card_pentacles_11
        "pentacles_12" -> R.drawable.tarot_card_pentacles_12
        "pentacles_13" -> R.drawable.tarot_card_pentacles_13
        "pentacles_14" -> R.drawable.tarot_card_pentacles_14

        else -> R.drawable.tarot_card_back
    }

    @DrawableRes
    fun cardDrawableRes(card: TarotCard): Int = cardDrawableRes(card.code)
}
