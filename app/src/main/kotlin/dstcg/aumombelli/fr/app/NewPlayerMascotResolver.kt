package fr.aumombelli.dstcg.app

import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.component.AsterAnchor
import fr.aumombelli.dstcg.ui.component.AsterFace
import fr.aumombelli.dstcg.ui.component.AsterHand
import fr.aumombelli.dstcg.ui.component.AsterHandSide
import fr.aumombelli.dstcg.ui.component.AsterMascotAspectRatio
import fr.aumombelli.dstcg.ui.component.AsterMascotScale
import fr.aumombelli.dstcg.ui.component.AsterMascotSpec
import fr.aumombelli.dstcg.ui.component.asterMascotWidthForContainer
import fr.aumombelli.dstcg.ui.motion.AppScene

internal fun resolveNewPlayerSceneMascotSpec(
    currentStep: NewPlayerOnboardingStep?,
    currentScene: AppScene,
    sceneState: AppSceneUiState,
    activeCoachmarkSpec: NewPlayerCoachmarkSpec?,
    badgeCelebrationVisible: Boolean,
): AsterMascotSpec? {
    if (
        currentStep == null ||
        currentStep == NewPlayerOnboardingStep.ShowConclusion ||
        currentStep == NewPlayerOnboardingStep.Completed ||
        sceneState.transitionLocked ||
        badgeCelebrationVisible ||
        !sceneState.onboardingHintsVisible
    ) {
        return null
    }
    val activeTextBoxTarget = activeCoachmarkSpec?.target ?: return null
    if (!sceneState.coachmarkTargetBounds.containsKey(activeTextBoxTarget)) {
        return null
    }

    return when (currentStep) {
        NewPlayerOnboardingStep.ShowWelcomeIntro -> null

        NewPlayerOnboardingStep.OpenFirstPackMenu ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.BigSmile,
                    hand = AsterHand.Point,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.SelectFirstExtension ->
            ifVisible(
                currentScene == AppScene.PackSelection &&
                    sceneState.packSceneVisible &&
                    sceneState.packExtensionListVisible,
            ) {
                mascot(
                    face = AsterFace.HappyEyes,
                    hand = AsterHand.Point,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.SelectFirstBooster ->
            ifVisible(currentScene == AppScene.PackSelection && sceneState.packSceneVisible) {
                mascot(
                    face = AsterFace.BigSmile,
                    hand = AsterHand.Point,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.ViewLibrary ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.Smile,
                    hand = AsterHand.Cards,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.LearnLibraryVariants -> null

        NewPlayerOnboardingStep.LearnCraftingTools ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.SmileLookingRight,
                    hand = AsterHand.Wrench,
                    anchor = AsterAnchor.BottomStart,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.ViewBadges ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.BigSmile,
                    hand = AsterHand.Point,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        -> null

        NewPlayerOnboardingStep.ViewEquipmentMenu ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.Smile,
                    hand = AsterHand.Telescope,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.ActivateFirstEquipment -> null

        NewPlayerOnboardingStep.ViewCraftingMenu ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.SmileLookingRight,
                    hand = AsterHand.Wrench,
                    anchor = AsterAnchor.BottomStart,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.UseSkyDarkening ->
            ifVisible(currentScene == AppScene.Crafting && sceneState.craftingContentVisible) {
                mascot(
                    face = AsterFace.BigSmile,
                    hand = AsterHand.Wrench,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.DiscoverMiniGames ->
            ifVisible(currentScene == AppScene.Home && sceneState.homeContentVisible) {
                mascot(
                    face = AsterFace.BigSmile,
                    hand = AsterHand.Point,
                    anchor = AsterAnchor.BottomEnd,
                ).hidingIfOverlappingTarget(sceneState, activeCoachmarkSpec)
            }

        NewPlayerOnboardingStep.ShowConclusion,
        NewPlayerOnboardingStep.Completed -> null
    }
}

internal fun resolveNewPlayerBlockingModalMascotSpec(
    kind: NewPlayerBlockingModalKind,
): AsterMascotSpec? = when (kind) {
    NewPlayerBlockingModalKind.WelcomeIntro -> mascot(
        face = AsterFace.Smile,
        hand = AsterHand.Open,
        anchor = AsterAnchor.BottomCenter,
        scale = AsterMascotScale.Compact,
        showBothHands = true,
        sizeMultiplier = CenteredModalAsterSizeMultiplier,
    )

    NewPlayerBlockingModalKind.Conclusion -> mascot(
        face = AsterFace.BigSmile,
        hand = AsterHand.Cards,
        anchor = AsterAnchor.BottomCenter,
        scale = AsterMascotScale.Compact,
        showBothHands = true,
        mirroredHand = AsterHand.Telescope,
        sizeMultiplier = CenteredModalAsterSizeMultiplier,
    )

    NewPlayerBlockingModalKind.LibraryVariants,
    NewPlayerBlockingModalKind.CraftingTools,
    -> null
}

private inline fun ifVisible(
    visible: Boolean,
    spec: () -> AsterMascotSpec?,
): AsterMascotSpec? = if (visible) spec() else null

private fun mascot(
    face: AsterFace,
    hand: AsterHand,
    anchor: AsterAnchor,
    scale: AsterMascotScale = AsterMascotScale.Standard,
    showBothHands: Boolean = false,
    mirroredHand: AsterHand? = null,
    sizeMultiplier: Float = 1f,
): AsterMascotSpec = AsterMascotSpec(
    face = face,
    hand = hand,
    handSide = when (anchor) {
        AsterAnchor.BottomStart -> AsterHandSide.Right
        AsterAnchor.BottomCenter,
        AsterAnchor.BottomEnd -> AsterHandSide.Left
    },
    anchor = anchor,
    scale = scale,
    showBothHands = showBothHands,
    mirroredHand = mirroredHand,
    sizeMultiplier = sizeMultiplier,
)

private fun AsterMascotSpec.hidingIfOverlappingTarget(
    sceneState: AppSceneUiState,
    activeCoachmarkSpec: NewPlayerCoachmarkSpec?,
): AsterMascotSpec? {
    val target = activeCoachmarkSpec?.target ?: return this
    return if (overlapsTarget(sceneState, target)) null else this
}

private const val CenteredModalAsterSizeMultiplier = 2f

private fun AsterMascotSpec.overlapsTarget(
    sceneState: AppSceneUiState,
    target: NewPlayerOnboardingTarget,
): Boolean {
    val targetBounds = sceneState.coachmarkTargetBounds[target] ?: return false
    val rootWidthPx = sceneState.rootWidthPx.takeIf { it > 0f } ?: return false
    val rootHeightPx = sceneState.rootHeightPx.takeIf { it > 0f } ?: return false
    val mascotWidthPx = asterMascotWidthForContainer(
        containerWidth = rootWidthPx,
        scale = scale,
        sizeMultiplier = sizeMultiplier,
    )
    val mascotHeightPx = mascotWidthPx / AsterMascotAspectRatio
    val marginPx = 10f
    val mascotLeft = when (anchor) {
        AsterAnchor.BottomStart -> marginPx
        AsterAnchor.BottomCenter -> (rootWidthPx - mascotWidthPx) / 2f
        AsterAnchor.BottomEnd -> rootWidthPx - mascotWidthPx - marginPx
    }
    val mascotTop = rootHeightPx - mascotHeightPx - marginPx
    val mascotRight = mascotLeft + mascotWidthPx
    val mascotBottom = mascotTop + mascotHeightPx
    val targetPaddingPx = 8f

    return mascotLeft < targetBounds.right + targetPaddingPx &&
        mascotRight > targetBounds.left - targetPaddingPx &&
        mascotTop < targetBounds.bottom + targetPaddingPx &&
        mascotBottom > targetBounds.top - targetPaddingPx
}
