package fr.aumombelli.dstcg.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AstronomyInfo(
    val commonName: String? = null,
    val primaryCatalogName: String,
    val catalogNumber: String,
    val objectFamily: String,
    val objectTypeLabel: String,
    val constellation: String,
    val mainSeason: String,
    val coordinates: CelestialCoordinates,
    val shortDescription: String,
    val details: AstronomyDetails,
)

@Serializable
data class CelestialCoordinates(
    val rightAscension: RightAscension,
    val declination: Declination,
    val label: String,
)

@Serializable
data class RightAscension(
    val hours: Int,
    val minutes: Int,
    val seconds: Double,
    val label: String,
)

@Serializable
data class Declination(
    val sign: String,
    val degrees: Int,
    val arcMinutes: Int,
    val arcSeconds: Int,
    val label: String,
)

@Serializable
data class LightYearMeasurement(
    val lightYears: Double,
    val label: String,
)

@Serializable
data class AngularMeasurement(
    val degrees: Int,
    val arcMinutes: Int,
    val arcSeconds: Int,
    val label: String,
)

@Serializable
data class VisualSize(
    val fullMoonWidth: Double,
    val fullMoonHeight: Double,
    val angularWidth: AngularMeasurement,
    val angularHeight: AngularMeasurement,
    val label: String,
)

@Serializable
data class AbsoluteMagnitudeMeasurement(
    val value: Double,
    val label: String,
)

@Serializable
sealed class AstronomyDetails

@Serializable
@SerialName("deep_sky")
data class DeepSkyDetails(
    val distance: LightYearMeasurement,
    val realSize: LightYearMeasurement,
    val visualSize: VisualSize,
    val absoluteMagnitude: AbsoluteMagnitudeMeasurement? = null,
) : AstronomyDetails()

@Serializable
@SerialName("star")
data class StarDetails(
    val distance: LightYearMeasurement,
    val realSize: LightYearMeasurement? = null,
    val visualSize: VisualSize? = null,
    val absoluteMagnitude: AbsoluteMagnitudeMeasurement,
) : AstronomyDetails()

@Serializable
@SerialName("constellation")
data class ConstellationDetails(
    val visualSize: VisualSize,
) : AstronomyDetails()

@Serializable
@SerialName("sky_event")
data class SkyEventDetails(
    val visualSize: VisualSize? = null,
) : AstronomyDetails()

@Serializable
@SerialName("solar_system")
data class SolarSystemDetails(
    val distance: LightYearMeasurement? = null,
    val realSize: LightYearMeasurement? = null,
    val absoluteMagnitude: AbsoluteMagnitudeMeasurement? = null,
) : AstronomyDetails()
