package fr.aumombelli.dstcg.feature.minigames

internal data class ObservatoryVisualReadiness(
    val alignmentReady: Boolean,
    val focusReady: Boolean,
)

internal fun observatoryVisualReadiness(
    azimuth: Float,
    altitude: Float,
    focus: Float,
    targetAzimuth: Float,
    targetAltitude: Float,
    targetFocus: Float,
    tolerance: Float,
): ObservatoryVisualReadiness {
    val visualAlignmentReady = isObservatorySettingReady(
        value = azimuth,
        target = targetAzimuth,
        tolerance = tolerance,
    ) && isObservatorySettingReady(
        value = altitude,
        target = targetAltitude,
        tolerance = tolerance,
    )
    return ObservatoryVisualReadiness(
        alignmentReady = visualAlignmentReady,
        focusReady = visualAlignmentReady && isObservatorySettingReady(
            value = focus,
            target = targetFocus,
            tolerance = tolerance,
        ),
    )
}
