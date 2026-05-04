package fr.aumombelli.dstcg.data

class EpicBoostManager(
    private val entropySource: EntropySource,
) {
    fun rollEpicBoostBoosterIndex(): Int? {
        if (!rollProbability(EpicBoostConfig.EPIC_BOOST_APPEARANCE_CHANCE)) {
            return null
        }
        return entropySource.nextInt(EpicBoostConfig.BOOSTERS_PER_EXTENSION)
    }

    private fun rollProbability(probability: Double): Boolean {
        if (probability <= 0.0) return false
        if (probability >= 1.0) return true
        val scale = 1_000_000
        val threshold = (probability * scale).toInt().coerceIn(1, scale)
        return entropySource.nextInt(scale) < threshold
    }
}
