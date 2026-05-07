package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningRevealSlotProbe
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.motion.MotionCard
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
fun PackSelectionScreen(
    state: PackSelectionUiState,
    onRefresh: () -> Unit,
    onSelectExtension: (String) -> Unit,
    onSelectBooster: (Int) -> Unit,
    onOpenPack: (String) -> Unit,
    onPackRevealReady: () -> Unit,
    onSelectedBoosterBoundsChanged: (PackRevealBounds?) -> Unit = {},
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    packReadySignal: Int,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    sceneVisible: Boolean = true,
    extensionListVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    backgroundOnly: Boolean = false,
    reserveBackButtonSpace: Boolean = false,
    backEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
) {
    val performanceProfile = LocalAppPerformanceProfile.current
    val shouldTickChargeStatus = state.rechargeState.availableDrawCount < state.maxStoredDraws
    val now by produceState(
        initialValue = state.trustedNow,
        state.rechargeState,
        state.trustedNow,
        state.trustedElapsedRealtimeMs,
        shouldTickChargeStatus,
    ) {
        value = computeTrustedNow(
            trustedNow = state.trustedNow,
            trustedElapsedRealtimeMs = state.trustedElapsedRealtimeMs,
        )
        while (true) {
            val delayMillis = if (shouldTickChargeStatus) {
                1_000L
            } else {
                millisUntilNextUtcDayStart(value).coerceAtLeast(1_000L)
            }
            delay(delayMillis)
            value = computeTrustedNow(
                trustedNow = state.trustedNow,
                trustedElapsedRealtimeMs = state.trustedElapsedRealtimeMs,
            )
        }
    }
    val liveChargeStatus = state.buildLiveChargeStatus(now)
    val weatherForecast = remember(now, state.weatherPolicy) {
        buildWeatherForecastDayUiModels(
            now = now,
            weatherPolicy = state.weatherPolicy,
        )
    }
    val weatherForecastUtcTimeLabel = remember(now) {
        formatWeatherForecastUtcTimeLabel(now)
    }
    val remainingDurationText = formatRemainingDuration(liveChargeStatus.remainingDuration)
    val drawLocked = liveChargeStatus.isDrawLocked
    val selectedExtension = state.extensions.firstOrNull { it.id == state.selectedExtensionId }
    var displayedExtensionId by remember(state.extensions) { mutableStateOf<String?>(selectedExtension?.id) }
    val displayedExtension = state.extensions.firstOrNull { it.id == displayedExtensionId }
    val displayedExtensionIndex = displayedExtension?.let { extension ->
        state.extensions.indexOfFirst { it.id == extension.id }.coerceAtLeast(0)
    } ?: 0
    val sceneAlpha by animateFloatAsState(
        targetValue = if (sceneVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "pack-selection-scene-alpha",
    )
    val extensionListAlpha by animateFloatAsState(
        targetValue = if (extensionListVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "pack-selection-extension-list-alpha",
    )
    val extensionListTranslationY by animateFloatAsState(
        targetValue = if (extensionListVisible) 0f else 280f,
        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
        label = "pack-selection-extension-list-translation",
    )
    var extensionListWasHidden by remember { mutableStateOf(!extensionListVisible) }
    var extensionListEntranceSignal by remember { mutableIntStateOf(0) }
    val activeExtensionListEntranceSignal = if (extensionListVisible && extensionListWasHidden) {
        extensionListEntranceSignal + 1
    } else {
        extensionListEntranceSignal
    }

    val heroProgress = remember { Animatable(0f) }
    val boosterIntroProgress = remember { Animatable(0f) }
    val boosterSelectionProgress = remember { Animatable(0f) }
    var screenBounds by remember { mutableStateOf<Rect?>(null) }
    var openingRevealTargetBounds by remember(displayedExtension?.id) { mutableStateOf<PackRevealBounds?>(null) }
    var handledPackSignal by remember(displayedExtension?.id) { mutableIntStateOf(packReadySignal) }

    LaunchedEffect(state.extensions, drawLocked) {
        if (state.extensions.isEmpty() || drawLocked) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionExtension, null)
        }
    }

    LaunchedEffect(extensionListVisible) {
        if (!extensionListVisible) {
            extensionListWasHidden = true
        } else if (extensionListWasHidden) {
            extensionListWasHidden = false
            extensionListEntranceSignal += 1
        }
    }

    LaunchedEffect(displayedExtensionId) {
        if (displayedExtensionId != null) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionExtension, null)
        }
    }

    LaunchedEffect(displayedExtension?.id) {
        if (displayedExtension == null) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionBooster, null)
        }
    }

    LaunchedEffect(selectedExtension?.id) {
        if (selectedExtension != null) {
            if (displayedExtensionId == selectedExtension.id && heroProgress.value >= 0.99f) return@LaunchedEffect
            displayedExtensionId = selectedExtension.id
            heroProgress.snapTo(0f)
            boosterIntroProgress.snapTo(0f)
            boosterSelectionProgress.snapTo(0f)
            heroProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
            )
            boosterIntroProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
            )
        } else if (displayedExtensionId != null) {
            boosterIntroProgress.snapTo(0f)
            boosterSelectionProgress.snapTo(0f)
            heroProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
            )
            displayedExtensionId = null
        }
    }

    LaunchedEffect(state.selectedBoosterIndex, displayedExtension?.id) {
        if (displayedExtension == null || state.selectedBoosterIndex == null) {
            boosterSelectionProgress.snapTo(0f)
            return@LaunchedEffect
        }
        boosterSelectionProgress.snapTo(0f)
        boosterSelectionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(packReadySignal, state.selectedBoosterIndex, displayedExtension?.id) {
        if (displayedExtension == null) return@LaunchedEffect
        if (state.selectedBoosterIndex == null) return@LaunchedEffect
        if (packReadySignal == handledPackSignal) return@LaunchedEffect
        handledPackSignal = packReadySignal
        val remainingSelectionMillis = (560f * (1f - boosterSelectionProgress.value))
            .coerceAtLeast(0f)
            .roundToLong()
        delay(remainingSelectionMillis + 80L)
        onPackRevealReady()
    }
    val listFadeProgress = if (displayedExtension != null) {
        (1f - heroProgress.value).coerceIn(0f, 1f)
    } else {
        1f
    }
    val introTextAlpha = if (displayedExtension != null) {
        (1f - heroProgress.value * 1.1f).coerceIn(0f, 1f)
    } else {
        1f
    }
    val stageTextAlpha = if (state.selectedBoosterIndex != null) {
        (1f - boosterSelectionProgress.value).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                screenBounds = coordinates.boundsInRoot()
            }
            .graphicsLayer {
                alpha = sceneAlpha
            }
            .background(
                if (showBackground) {
                    Brush.verticalGradient(listOf(Color(0x5509111E), Color(0x88060B12)))
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                },
            )
            .testTag("pack-screen-root"),
    ) {
        if (!backgroundOnly) {
            displayedExtension?.let { extension ->
                PackOpeningRevealSlotProbe(
                    extensionLabel = extension.name,
                    totalItems = 1,
                    onBoundsChanged = { bounds ->
                        openingRevealTargetBounds = bounds
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .dstcgContentInsetsPadding(includeBottom = true)
                    .padding(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        alpha = stageTextAlpha
                    },
                ) {
                    onBack?.let { back ->
                        key(state.selectedExtensionId) {
                            SceneNavigationButton(
                                icon = SceneNavigationIcon.Back,
                                onClick = back,
                                contentDescription = "Retour",
                                testTag = "pack-back",
                                enabled = backEnabled,
                            )
                        }
                    } ?: run {
                        if (reserveBackButtonSpace) {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                    Text(
                        text = "Ouvrir un pack",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.testTag("pack-title"),
                    )
                }

                PackChargeStatus(
                    availableDrawCount = liveChargeStatus.availableDrawCount,
                    maxStoredDraws = liveChargeStatus.maxStoredDraws,
                    rechargeProgress = liveChargeStatus.rechargeProgress,
                    remainingDurationText = remainingDurationText,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = stageTextAlpha
                        }
                        .testTag("pack-status"),
                )

                WeatherForecastCard(
                    forecast = weatherForecast,
                    utcTimeLabel = weatherForecastUtcTimeLabel,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = stageTextAlpha
                        }
                        .testTag("pack-weather-forecast"),
                )

                state.errorMessage?.let { error ->
                    MotionCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(18.dp),
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFFFB1B1),
                                modifier = Modifier.testTag("pack-error"),
                            )
                            Button(
                                onClick = onRefresh,
                                enabled = interactionsEnabled,
                                modifier = Modifier.testTag("pack-refresh"),
                            ) {
                                Text("Réessayer")
                            }
                        }
                    }
                }

                if (state.isLoading && state.extensions.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        modifier = Modifier.weight(1f, fill = true),
                    ) {
                        Text(
                            text = "Choisis l'extension a contempler.",
                            color = Color(0xFFEAF4FF),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.graphicsLayer {
                                alpha = introTextAlpha
                            },
                        )
                        ExtensionList(
                            extensions = state.extensions,
                            drawLocked = drawLocked,
                            onSelectExtension = { extensionId ->
                                onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionExtension, null)
                                onSelectExtension(extensionId)
                            },
                            interactionsEnabled = interactionsEnabled && displayedExtension == null,
                            highlightedExtensionId = displayedExtensionId,
                            highlightProgress = heroProgress.value,
                            badgeAnimationsEnabled = !performanceProfile.isLowRamDevice &&
                                extensionListVisible &&
                                displayedExtension == null,
                            onFirstEnabledExtensionBoundsChanged = { bounds ->
                                onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionExtension, bounds)
                            },
                            entranceSignal = activeExtensionListEntranceSignal,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = EXTENSION_LIST_TOP_PADDING)
                                .graphicsLayer {
                                    alpha = extensionListAlpha * listFadeProgress
                                    translationY = extensionListTranslationY
                                },
                        )
                        displayedExtension?.let { extension ->
                            ExtensionBoosterStage(
                                extension = extension,
                                extensionIndex = displayedExtensionIndex,
                                heroProgress = heroProgress.value,
                                boosterIntroProgress = boosterIntroProgress.value,
                                boosterSelectionProgress = boosterSelectionProgress.value,
                                drawLocked = drawLocked,
                                selectedBoosterIndex = state.selectedBoosterIndex,
                                epicBoostBoosterIndex = state.epicBoostBoosterIndex,
                                isAwaitingPackResult = state.isAwaitingPackResult,
                                screenBounds = screenBounds,
                                selectedBoosterTargetBounds = openingRevealTargetBounds,
                                onSelectBooster = { boosterIndex ->
                                    onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionBooster, null)
                                    onSelectBooster(boosterIndex)
                                    onOpenPack(extension.id)
                                },
                                onSelectedBoosterBoundsChanged = onSelectedBoosterBoundsChanged,
                                onBoosterCoachmarkBoundsChanged = { bounds ->
                                    onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.PackSelectionBooster, bounds)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        } else {
            displayedExtension?.let { extension ->
                ExtensionBoosterStage(
                    extension = extension,
                    extensionIndex = displayedExtensionIndex,
                    heroProgress = heroProgress.value,
                    boosterIntroProgress = boosterIntroProgress.value,
                    boosterSelectionProgress = boosterSelectionProgress.value,
                    drawLocked = drawLocked,
                    selectedBoosterIndex = state.selectedBoosterIndex,
                    epicBoostBoosterIndex = state.epicBoostBoosterIndex,
                    isAwaitingPackResult = state.isAwaitingPackResult,
                    screenBounds = screenBounds,
                    selectedBoosterTargetBounds = null,
                    onSelectBooster = {},
                    onSelectedBoosterBoundsChanged = {},
                    backgroundOnly = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
