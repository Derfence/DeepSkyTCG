package fr.aumombelli.dstcg.feature.home

import fr.aumombelli.dstcg.audio.AudioCreditEntry

internal data class HomeAudioCreditDisplayEntry(
    val fileName: String,
    val title: String,
    val artist: String,
    val license: String,
    val sourcePage: String?,
    val changes: String?,
)

internal fun homeAudioCreditDisplayEntries(
    audioCredits: List<AudioCreditEntry>,
): List<HomeAudioCreditDisplayEntry> =
    audioCredits
        .filter { it.hasVisibleCredit }
        .map { entry ->
            HomeAudioCreditDisplayEntry(
                fileName = entry.fileName,
                title = entry.title.cleanOrNull() ?: entry.usage.cleanOrNull() ?: entry.fileName,
                artist = entry.artist.cleanOrNull() ?: "artiste à renseigner",
                license = entry.license.cleanOrNull() ?: "licence à renseigner",
                sourcePage = entry.sourcePage.cleanOrNull(),
                changes = entry.changes.cleanOrNull(),
            )
        }

private val AudioCreditEntry.hasVisibleCredit: Boolean
    get() = listOf(title, artist, license, licenseUrl, sourcePage, downloadedAt, changes, notes)
        .any { it.cleanOrNull() != null }

private fun String?.cleanOrNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }
