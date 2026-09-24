package com.samge.qanvas.core

import com.scsonic.qwenimage21.QwenImage21

/**
 * Reflection-free proxy over the runtime's Size API, so the UI layer never touches
 * AAR classes directly. Ordinals are stable (enum order in the AAR).
 */
object QwenImage21SizeProxy {
    val RATIOS: Array<QwenImage21.Size.Ratio> = QwenImage21.Size.Ratio.values()
    val TIERS: Array<QwenImage21.Size.Tier> = QwenImage21.Size.Tier.values()

    fun of(ratioOrd: Int, tierOrd: Int): QwenImage21.Size =
        QwenImage21.Size.of(
            RATIOS[ratioOrd.coerceIn(0, RATIOS.size - 1)],
            TIERS[tierOrd.coerceIn(0, TIERS.size - 1)],
        )

    /** Exact edit output for a src image at a tier. */
    fun editSize(srcW: Int, srcH: Int, tierOrd: Int): IntArray =
        QwenImage21.editSize(srcW, srcH, TIERS[tierOrd.coerceIn(0, TIERS.size - 1)])
}
