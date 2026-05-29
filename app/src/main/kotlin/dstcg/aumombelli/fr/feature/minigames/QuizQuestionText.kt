package fr.aumombelli.dstcg.feature.minigames

import java.text.Normalizer
import java.util.Locale

internal fun List<String>.distinctQuizAnswers(): List<String> {
    val seen = mutableSetOf<String>()
    return mapNotNull { answer ->
        val clean = answer.cleanAnswer() ?: return@mapNotNull null
        clean.takeIf { seen.add(it.normalizedQuizAnswer()) }
    }
}

internal fun String?.cleanAnswer(): String? = this
    ?.trim()
    ?.replace(WhitespaceRegex, " ")
    ?.takeIf { it.isNotEmpty() }

internal fun String.normalizedQuizAnswer(): String {
    val folded = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(CombiningMarkRegex, "")
    return folded
        .lowercase(Locale.ROOT)
        .replace("’", "'")
        .replace(WhitespaceRegex, " ")
        .trim()
}

private val WhitespaceRegex = Regex("\\s+")
private val CombiningMarkRegex = Regex("\\p{Mn}+")
