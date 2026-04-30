package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onRefresh: () -> Unit,
    onActivateEquipment: (String) -> Unit,
    contentVisible: Boolean = true,
    onOnboardingActivationBoundsChanged: (Rect?) -> Unit = {},
    onOnboardingActivationScrollHintChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cardsById = remember(state.sections) {
        state.sections
            .flatMap { it.cards }
            .associateBy { it.definition.id }
    }
    var openedCardId by remember(state.sections) { mutableStateOf<String?>(null) }
    val openedCard = openedCardId?.let(cardsById::get)
    val closeOpenedCard = {
        openedCardId = null
    }

    val onboardingActivationTargetEnabled =
        contentVisible &&
            state.activatingCardId == null &&
            state.activeEffects.isEmpty()
    val listCoordinatesHolder = remember { LayoutCoordinatesHolder() }
    val listState = rememberLazyListState()
    val firstActivatableCardId = state.sections.asSequence()
        .flatMap { it.cards.asSequence() }
        .firstOrNull { it.activationEnabled && onboardingActivationTargetEnabled }
        ?.definition
        ?.id
    val firstActivatableSectionIndex = state.sections.indexOfFirst { section ->
        section.cards.any { it.definition.id == firstActivatableCardId }
    }.takeIf { it >= 0 }
    val sectionListStartIndex = 1 +
        (if (state.isLoading) 1 else 0) +
        (if (state.errorMessage != null) 1 else 0) +
        1
    val firstActivatableSectionItemIndex = firstActivatableSectionIndex?.let(sectionListStartIndex::plus)
    val maxVisibleListItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index }
    val activationTargetEnabled = firstActivatableCardId != null && onboardingActivationTargetEnabled
    val targetSectionOffscreenBelow =
        firstActivatableSectionItemIndex != null &&
            maxVisibleListItemIndex != null &&
            firstActivatableSectionItemIndex > maxVisibleListItemIndex
    LaunchedEffect(
        firstActivatableCardId,
        onboardingActivationTargetEnabled,
        firstActivatableSectionItemIndex,
        maxVisibleListItemIndex,
    ) {
        when {
            !activationTargetEnabled -> {
                onOnboardingActivationBoundsChanged(null)
                onOnboardingActivationScrollHintChanged(false)
            }

            targetSectionOffscreenBelow -> {
                onOnboardingActivationBoundsChanged(null)
                onOnboardingActivationScrollHintChanged(true)
            }
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 880, easing = FastOutSlowInEasing),
        label = "equipment-content-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07121F),
                        Color(0xFF10233D),
                        Color(0xFF193555),
                    ),
                ),
            )
            .testTag("equipment-screen"),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true)
                .onGloballyPositioned { coordinates ->
                    listCoordinatesHolder.value = coordinates
                }
                .testTag("equipment-list"),
        ) {
            item(key = "equipment-header") {
                EquipmentHeader(
                    errorMessage = state.errorMessage,
                    onRefresh = onRefresh,
                )
            }

            if (state.isLoading) {
                item(key = "equipment-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { error ->
                item(key = "equipment-error") {
                    Text(
                        text = error,
                        color = Color(0xFFFFA3A3),
                        modifier = Modifier.testTag("equipment-error"),
                    )
                }
            }

            item(key = "equipment-active-summary") {
                EquipmentActiveSummaryCard(activeEffects = state.activeEffects)
            }

            items(state.sections, key = { it.type.code }) { section ->
                EquipmentSectionCard(
                    section = section,
                    activatingCardId = state.activatingCardId,
                    onPreviewEquipment = { equipmentCardId ->
                        openedCardId = equipmentCardId
                    },
                    onActivateEquipment = onActivateEquipment,
                    firstActivatableCardId = firstActivatableCardId,
                    contentVisible = contentVisible,
                    listCoordinatesProvider = { listCoordinatesHolder.value },
                    onOnboardingActivationBoundsChanged = onOnboardingActivationBoundsChanged,
                    onOnboardingActivationScrollHintChanged = onOnboardingActivationScrollHintChanged,
                )
            }
        }

        if (openedCard != null) {
            EquipmentCardFullscreenDialog(
                card = openedCard,
                onDismiss = closeOpenedCard,
            )
        }
    }
}
