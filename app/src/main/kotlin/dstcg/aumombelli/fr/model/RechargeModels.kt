package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

/**
 * Canonical persisted state for pack recharge.
 *
 * `availableDrawCount` stores the immediately usable stock. The remaining
 * fields keep the exact recharge progression in integer units so weather
 * multipliers such as `x0.8` stay deterministic and drift-free.
 */
@Serializable
data class PackRechargeState(
    val availableDrawCount: Int = 10,
    val accumulatedChargeUnits: Long = 0L,
    val lastChargeEvaluationAt: String? = null,
)
