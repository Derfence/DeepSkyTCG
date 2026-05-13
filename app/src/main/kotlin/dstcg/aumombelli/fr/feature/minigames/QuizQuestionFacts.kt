package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.AstronomyDetails
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ConstellationDetails
import fr.aumombelli.dstcg.model.Declination
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.LightYearMeasurement
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StarDetails
import fr.aumombelli.dstcg.model.VisualSize

internal data class QuizFactSet(
    val cardId: String,
    val cardName: String,
    val profile: QuizObjectProfile,
    val objectType: String,
    val familyLabel: String,
    val constellation: String?,
    val mainSeason: String,
    val primaryCatalog: String,
    val catalogNumber: String,
    val celestialHemisphere: String,
    val distanceScale: QuizScaleFact?,
    val visualMoonScale: String?,
    val realSizeScale: QuizScaleFact?,
    val magnitudeClass: String?,
    val profileCategory: String,
    val solarSystemDistanceContext: String?,
) {
    companion object {
        fun from(card: CardDefinition): QuizFactSet {
            val astronomy = card.astronomy
            val profile = QuizObjectProfile.from(astronomy.details, astronomy.objectFamily)
            val constellation = astronomy.constellation.cleanAnswer()
            val meaningfulConstellation = constellation
                ?.takeUnless { profile == QuizObjectProfile.SolarSystem && it.normalizedQuizAnswer() == "variable" }
            val details = astronomy.details
            val distance = details.distanceMeasurement()
            val realSize = details.realSizeMeasurement()
            val visualSize = details.visualSizeMeasurement()
            val magnitudeValue = details.absoluteMagnitudeValue()
            return QuizFactSet(
                cardId = card.id,
                cardName = astronomy.commonName.cleanAnswer() ?: card.name.cleanAnswer() ?: card.id,
                profile = profile,
                objectType = astronomy.objectTypeLabel.cleanAnswer() ?: "Objet astronomique",
                familyLabel = profile.familyLabel,
                constellation = meaningfulConstellation,
                mainSeason = astronomy.mainSeason.cleanAnswer() ?: "Toute l'année",
                primaryCatalog = astronomy.primaryCatalogName.cleanAnswer() ?: "Catalogue principal",
                catalogNumber = astronomy.catalogNumber.cleanAnswer() ?: card.id,
                celestialHemisphere = astronomy.coordinates.declination.toHemisphereAnswer(),
                distanceScale = distance?.toDistanceScale(profile),
                visualMoonScale = visualSize?.toVisualMoonScale(),
                realSizeScale = realSize?.toRealSizeScale(profile),
                magnitudeClass = magnitudeValue
                    ?.takeIf { profile == QuizObjectProfile.Star || profile == QuizObjectProfile.DeepSky }
                    ?.toMagnitudeClass(),
                profileCategory = profile.categoryAnswer,
                solarSystemDistanceContext = distance?.label?.toSolarSystemDistanceContext()
                    ?.takeIf { profile == QuizObjectProfile.SolarSystem },
            )
        }
    }
}

internal data class QuizScaleFact(
    val answer: String,
    val unit: QuizScaleUnit,
)

internal enum class QuizScaleUnit {
    Kilometers,
    LightYears,
}

internal enum class QuizObjectProfile(
    val familyLabel: String,
    val categoryAnswer: String,
) {
    SolarSystem(
        familyLabel = "Système solaire",
        categoryAnswer = "un objet ou phénomène du Système solaire",
    ),
    DeepSky(
        familyLabel = "Ciel profond",
        categoryAnswer = "un objet du ciel profond",
    ),
    Star(
        familyLabel = "Étoile",
        categoryAnswer = "une étoile ou un système stellaire",
    ),
    Constellation(
        familyLabel = "Constellation",
        categoryAnswer = "une constellation",
    ),
    SkyEvent(
        familyLabel = "Événement céleste",
        categoryAnswer = "un événement céleste observable",
    ),
    Generic(
        familyLabel = "Objet astronomique",
        categoryAnswer = "un objet astronomique",
    );

    companion object {
        fun from(details: AstronomyDetails, objectFamily: String): QuizObjectProfile = when (details) {
            is SolarSystemDetails -> SolarSystem
            is DeepSkyDetails -> DeepSky
            is StarDetails -> Star
            is ConstellationDetails -> Constellation
            is SkyEventDetails -> SkyEvent
            else -> when (objectFamily.normalizedQuizAnswer()) {
                "solar_system" -> SolarSystem
                "deep_sky" -> DeepSky
                "star" -> Star
                "constellation" -> Constellation
                "sky_event" -> SkyEvent
                else -> Generic
            }
        }
    }
}

private fun AstronomyDetails.distanceMeasurement(): LightYearMeasurement? = when (this) {
    is DeepSkyDetails -> distance
    is StarDetails -> distance
    is SolarSystemDetails -> distance
    else -> null
}

private fun AstronomyDetails.realSizeMeasurement(): LightYearMeasurement? = when (this) {
    is DeepSkyDetails -> realSize
    is StarDetails -> realSize
    is SolarSystemDetails -> realSize
    else -> null
}

private fun AstronomyDetails.visualSizeMeasurement(): VisualSize? = when (this) {
    is DeepSkyDetails -> visualSize
    is StarDetails -> visualSize
    is ConstellationDetails -> visualSize
    is SkyEventDetails -> visualSize
    else -> null
}

private fun AstronomyDetails.absoluteMagnitudeValue(): Double? = when (this) {
    is DeepSkyDetails -> absoluteMagnitude?.value
    is StarDetails -> absoluteMagnitude.value
    is SolarSystemDetails -> absoluteMagnitude?.value
    else -> null
}

private fun LightYearMeasurement.toDistanceScale(profile: QuizObjectProfile): QuizScaleFact? {
    if (profile == QuizObjectProfile.SolarSystem) return null
    val answer = when {
        lightYears < 10.0 -> "moins de 10 années-lumière"
        lightYears < 100.0 -> "quelques dizaines d'années-lumière"
        lightYears < 1_000.0 -> "quelques centaines d'années-lumière"
        lightYears < 10_000.0 -> "quelques milliers d'années-lumière"
        lightYears < 1_000_000.0 -> "des dizaines de milliers d'années-lumière"
        else -> "des millions d'années-lumière"
    }
    return QuizScaleFact(answer = answer, unit = QuizScaleUnit.LightYears)
}

private fun LightYearMeasurement.toRealSizeScale(profile: QuizObjectProfile): QuizScaleFact {
    val isSolarSystemScale = profile == QuizObjectProfile.SolarSystem || label.normalizedQuizAnswer().contains("km")
    return if (isSolarSystemScale) {
        QuizScaleFact(
            answer = when {
                lightYears < 100.0 -> "moins de 100 km"
                lightYears < 1_000.0 -> "quelques centaines de kilomètres"
                lightYears < 10_000.0 -> "quelques milliers de kilomètres"
                lightYears < 50_000.0 -> "des dizaines de milliers de kilomètres"
                else -> "plus de 50 000 km"
            },
            unit = QuizScaleUnit.Kilometers,
        )
    } else {
        QuizScaleFact(
            answer = when {
                lightYears < 0.01 -> "moins d'un centième d'année-lumière"
                lightYears < 1.0 -> "moins d'une année-lumière"
                lightYears < 10.0 -> "quelques années-lumière"
                lightYears < 100.0 -> "des dizaines d'années-lumière"
                else -> "des milliers d'années-lumière ou plus"
            },
            unit = QuizScaleUnit.LightYears,
        )
    }
}

private fun VisualSize.toVisualMoonScale(): String {
    val fullMoonSpan = maxOf(fullMoonWidth, fullMoonHeight)
    return when {
        fullMoonSpan < 0.25 -> "bien plus petite que la pleine Lune"
        fullMoonSpan < 0.8 -> "plus petite que la pleine Lune"
        fullMoonSpan <= 1.5 -> "comparable à la pleine Lune"
        fullMoonSpan <= 5.0 -> "plus grande que la pleine Lune"
        else -> "beaucoup plus étendue que la pleine Lune"
    }
}

private fun Double.toMagnitudeClass(): String = when {
    this <= -20.0 -> "extrêmement lumineuse à l'échelle galactique"
    this <= -8.0 -> "très lumineuse intrinsèquement"
    this <= -2.0 -> "lumineuse intrinsèquement"
    this <= 2.0 -> "de luminosité intrinsèque modérée"
    else -> "peu lumineuse intrinsèquement"
}

private fun Declination.toHemisphereAnswer(): String {
    val totalArcSeconds = degrees + arcMinutes + arcSeconds
    return when {
        totalArcSeconds == 0 -> "Équateur céleste"
        sign == "-" -> "Hémisphère céleste sud"
        else -> "Hémisphère céleste nord"
    }
}

private fun String.toSolarSystemDistanceContext(): String? {
    val normalized = normalizedQuizAnswer()
    return when {
        "ua" in normalized -> "une distance au Soleil en unités astronomiques"
        "km de" in normalized -> "une distance à un astre parent en kilomètres"
        "km" in normalized -> "une distance locale exprimée en kilomètres"
        else -> null
    }
}
