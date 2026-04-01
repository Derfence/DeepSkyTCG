package fr.aumombelli.gatcha.feature.badges

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.ui.screen.gatchaContentInsetsPadding
import kotlinx.coroutines.delay

@Composable
fun BadgeBookScreen(
    state: BadgeBookUiState,
    onRefresh: () -> Unit,
    contentVisible: Boolean = true,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 920, easing = FastOutSlowInEasing),
        label = "badge-book-content-alpha",
    )
    val badgeBounds = remember(state.sections) { mutableStateMapOf<String, Rect>() }
    var activeDetail by remember(state.sections) { mutableStateOf<ActiveBadgeDetail?>(null) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    BackHandler(enabled = activeDetail != null) {
        if (activeDetail?.isExpanded == true) {
            activeDetail = activeDetail?.copy(isExpanded = false)
        }
    }

    LaunchedEffect(activeDetail?.badge?.id, activeDetail?.hasEntered, activeDetail?.isExpanded) {
        val detail = activeDetail ?: return@LaunchedEffect
        when {
            !detail.hasEntered -> {
                activeDetail = detail.copy(
                    isExpanded = true,
                    hasEntered = true,
                )
            }

            !detail.isExpanded -> {
                delay(BadgeDetailAnimationDurationMillis.toLong())
                if (activeDetail?.badge?.id == detail.badge.id && activeDetail?.isExpanded == false) {
                    activeDetail = null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF130D10),
                        Color(0xFF0A1019),
                        Color(0xFF0B1524),
                    ),
                ),
            )
            .onSizeChanged { size -> rootSize = size },
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .gatchaContentInsetsPadding(includeBottom = true)
                .testTag("badge-book-scroll"),
        ) {
            item(key = "badge-book-header") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Carnet de badges",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (state.errorMessage != null) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("badge-book-refresh"),
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
            }

            if (state.isLoading) {
                item(key = "badge-book-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { error ->
                item(key = "badge-book-error") {
                    Text(
                        text = error,
                        color = Color(0xFFFF9B9B),
                        modifier = Modifier.testTag("badge-book-error"),
                    )
                }
            }

            items(state.sections, key = { it.extensionId }) { section ->
                BadgeSectionCard(
                    section = section,
                    onBadgeClick = { badge ->
                        val bounds = badgeBounds[badge.id] ?: return@BadgeSectionCard
                        activeDetail = ActiveBadgeDetail(
                            badge = badge,
                            sourceBounds = bounds,
                        )
                    },
                    hiddenBadgeId = activeDetail?.badge?.id,
                    onBadgePositioned = { badgeId, bounds -> badgeBounds[badgeId] = bounds },
                )
            }
        }

        activeDetail?.let { detail ->
            BadgeDetailOverlay(
                detail = detail.copy(
                    sourceBounds = badgeBounds[detail.badge.id] ?: detail.sourceBounds,
                ),
                rootSize = rootSize,
                onDismiss = {
                    if (activeDetail?.isExpanded == true) {
                        activeDetail = activeDetail?.copy(isExpanded = false)
                    }
                },
            )
        }
    }
}

@Composable
private fun BadgeSectionCard(
    section: BadgeSection,
    onBadgeClick: (BadgeItem) -> Unit,
    hiddenBadgeId: String?,
    onBadgePositioned: (String, Rect) -> Unit,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.24f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("badge-section-${section.extensionId}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88202736),
                            Color(0x55212638),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = section.extensionName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${section.unlockedCount} / ${section.badges.size} badges obtenus",
                    color = Color(0xFFF0D995),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val spacing = 12.dp
                val cellWidth = ((maxWidth - spacing * 2) / 3f).coerceAtLeast(84.dp)
                val coinSize = (cellWidth * 0.74f).coerceIn(64.dp, 92.dp)
                val perfectBadge = section.badges.firstOrNull {
                    section.sectionType == BadgeSectionType.Extension &&
                    it.requirementType == BadgeRequirementType.PerfectCollection
                }
                val regularRows = section.badges
                    .filterNot {
                        section.sectionType == BadgeSectionType.Extension &&
                            it.requirementType == BadgeRequirementType.PerfectCollection
                    }
                    .chunked(3)

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    regularRows.forEach { rowBadges ->
                        BadgeGridRow(
                            badges = rowBadges,
                            cellWidth = cellWidth,
                            coinSize = coinSize,
                            hiddenBadgeId = hiddenBadgeId,
                            onBadgePositioned = onBadgePositioned,
                            onBadgeClick = onBadgeClick,
                        )
                    }

                    perfectBadge?.let { badge ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            BadgeCoinCard(
                                badge = badge,
                                coinSize = coinSize,
                                isCoinHidden = hiddenBadgeId == badge.id,
                                onCoinPositioned = { bounds -> onBadgePositioned(badge.id, bounds) },
                                onClick = { onBadgeClick(badge) },
                                modifier = Modifier.width(cellWidth),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeGridRow(
    badges: List<BadgeItem>,
    cellWidth: androidx.compose.ui.unit.Dp,
    coinSize: androidx.compose.ui.unit.Dp,
    hiddenBadgeId: String?,
    onBadgePositioned: (String, Rect) -> Unit,
    onBadgeClick: (BadgeItem) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        badges.forEach { badge ->
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.width(cellWidth),
            ) {
                BadgeCoinCard(
                    badge = badge,
                    coinSize = coinSize,
                    isCoinHidden = hiddenBadgeId == badge.id,
                    onCoinPositioned = { bounds -> onBadgePositioned(badge.id, bounds) },
                    onClick = { onBadgeClick(badge) },
                )
            }
        }
    }
}
