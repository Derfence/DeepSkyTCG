package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.AssetSvgImage
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import fr.aumombelli.dstcg.ui.theme.AuroraTeal

@Composable
internal fun MiniGamesMenuScreen(
    state: MiniGamesUiState,
    onBack: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenObservatory: () -> Unit,
    contentVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "mini-games-menu-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .testTag("mini-games-menu-screen"),
    ) {
        MiniGameSceneBackdrop(
            variant = SkyBackdropVariant.Suburban,
            sparkleBoost = 0.12f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mini-games-map"),
        ) {
            MiniGamesFallbackMap(modifier = Modifier.fillMaxSize())
            AssetSvgImage(
                assetPath = MiniGamesMapSvgAssetName,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            MiniGameMapNode(
                index = "1",
                title = "Quiz universitaire",
                status = state.quizStatusLabel,
                anchorX = MiniGamesQuizAnchorX,
                anchorY = MiniGamesQuizAnchorY,
                buttonState = when {
                    state.isLoading -> MiniGameMenuButtonState.Loading
                    state.quizPlayedToday -> MiniGameMenuButtonState.Consumed
                    else -> MiniGameMenuButtonState.Available
                },
                interactionsEnabled = interactionsEnabled && !state.isLoading,
                onClick = onOpenQuiz,
                testTag = "mini-games-quiz",
            )
            MiniGameMapNode(
                index = "2",
                title = "Memory amateur",
                status = state.memoryStatusLabel,
                anchorX = MiniGamesMemoryAnchorX,
                anchorY = MiniGamesMemoryAnchorY,
                buttonState = when {
                    state.isLoading -> MiniGameMenuButtonState.Loading
                    state.memoryPlayedToday -> MiniGameMenuButtonState.Consumed
                    else -> MiniGameMenuButtonState.Available
                },
                interactionsEnabled = interactionsEnabled && !state.isLoading,
                onClick = onOpenMemory,
                testTag = "mini-games-memory",
            )
            MiniGameMapNode(
                index = "3",
                title = "Comparaison",
                status = state.timelineStatusLabel,
                anchorX = MiniGamesTimelineAnchorX,
                anchorY = MiniGamesTimelineAnchorY,
                buttonState = when {
                    state.isLoading -> MiniGameMenuButtonState.Loading
                    state.timelinePlayedToday -> MiniGameMenuButtonState.Consumed
                    else -> MiniGameMenuButtonState.Available
                },
                interactionsEnabled = interactionsEnabled && !state.isLoading,
                onClick = onOpenTimeline,
                testTag = "mini-games-timeline",
            )
            MiniGameMapNode(
                index = "4",
                title = "Observatoire",
                status = state.observatoryStatusLabel,
                anchorX = MiniGamesObservatoryAnchorX,
                anchorY = MiniGamesObservatoryAnchorY,
                buttonState = when {
                    state.isLoading -> MiniGameMenuButtonState.Loading
                    state.observatoryPlayedToday -> MiniGameMenuButtonState.Consumed
                    else -> MiniGameMenuButtonState.Available
                },
                interactionsEnabled = interactionsEnabled && !state.isLoading,
                onClick = onOpenObservatory,
                testTag = "mini-games-observatory",
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = false)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            SceneNavigationButton(
                icon = SceneNavigationIcon.Back,
                onClick = onBack,
                contentDescription = "Retour",
                testTag = "mini-games-menu-back",
                enabled = interactionsEnabled,
                modifier = Modifier.align(Alignment.TopStart),
            )

            Text(
                text = "Mini-jeux",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )

            state.errorMessage?.let { message ->
                Surface(
                    color = Color(0xCC180B0D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(18.dp),
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFFFC4BD),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGameMapNode(
    index: String,
    title: String,
    status: String,
    anchorX: Float,
    anchorY: Float,
    buttonState: MiniGameMenuButtonState,
    interactionsEnabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val buttonSize = 52.dp
        val calloutGap = 12.dp
        val buttonEnabled = interactionsEnabled && buttonState.isInteractive
        val buttonColors = buttonState.colors()
        val pulseTone = when (buttonState) {
            MiniGameMenuButtonState.Available -> MiniGameFeedbackTone.Completion
            MiniGameMenuButtonState.Consumed -> MiniGameFeedbackTone.Success
            MiniGameMenuButtonState.Loading -> MiniGameFeedbackTone.Success
            MiniGameMenuButtonState.Disabled -> MiniGameFeedbackTone.Success
        }
        val anchorCenterX = maxWidth * anchorX
        val anchorCenterY = maxHeight * anchorY
        val leftAvailableSpace = anchorCenterX - (buttonSize / 2f)
        val rightAvailableSpace = maxWidth - anchorCenterX - (buttonSize / 2f)
        val placeCalloutRight = rightAvailableSpace >= leftAvailableSpace
        val calloutTextAlign = if (placeCalloutRight) TextAlign.Start else TextAlign.End
        val calloutWidth = minOf(maxWidth * 0.38f, 220.dp)
        val calloutMaxX = maxOf(0.dp, maxWidth - calloutWidth)
        val calloutRawX = if (placeCalloutRight) {
            anchorCenterX + (buttonSize / 2f) + calloutGap
        } else {
            anchorCenterX - (buttonSize / 2f) - calloutGap - calloutWidth
        }
        val calloutX = calloutRawX.coerceIn(0.dp, calloutMaxX)
        val calloutY = (anchorCenterY - 28.dp).coerceIn(0.dp, maxOf(0.dp, maxHeight - 64.dp))
        val semanticState = when (buttonState) {
            MiniGameMenuButtonState.Available -> "Disponible"
            MiniGameMenuButtonState.Consumed -> "Essai quotidien consommé"
            MiniGameMenuButtonState.Loading -> "Chargement"
            MiniGameMenuButtonState.Disabled -> "Indisponible"
        }

        if (buttonState == MiniGameMenuButtonState.Available || buttonState == MiniGameMenuButtonState.Consumed) {
            val ringSize = buttonSize + 24.dp
            MiniGamePulsingRing(
                enabled = buttonState == MiniGameMenuButtonState.Available && interactionsEnabled,
                tone = pulseTone,
                modifier = Modifier
                    .offset(
                        x = anchorCenterX - (ringSize / 2f),
                        y = anchorCenterY - (ringSize / 2f),
                    )
                    .size(ringSize),
            )
        }
        MiniGameInfoCallout(
            title = title,
            status = status,
            borderColor = buttonColors.calloutBorder,
            textAlign = calloutTextAlign,
            modifier = Modifier
                .offset(x = calloutX, y = calloutY)
                .width(calloutWidth)
                .heightIn(min = 52.dp)
                .testTag("$testTag-info"),
        )
        Surface(
            onClick = onClick,
            enabled = buttonEnabled,
            shape = CircleShape,
            color = buttonColors.button,
            contentColor = buttonColors.content,
            shadowElevation = if (buttonEnabled) 12.dp else 8.dp,
            modifier = Modifier
                .offset(
                    x = anchorCenterX - (buttonSize / 2f),
                    y = anchorCenterY - (buttonSize / 2f),
                )
                .size(buttonSize)
                .semantics { stateDescription = semanticState }
                .testTag(testTag),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = index,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = buttonColors.content,
                )
            }
        }
    }
}

@Composable
private fun MiniGamesFallbackMap(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF18202D),
                    Color(0xFF0A1522),
                    Color(0xFF05080E),
                ),
            ),
        )
        val points = listOf(
            Offset(size.width * MiniGamesQuizAnchorX, size.height * MiniGamesQuizAnchorY),
            Offset(size.width * MiniGamesMemoryAnchorX, size.height * MiniGamesMemoryAnchorY),
            Offset(size.width * MiniGamesTimelineAnchorX, size.height * MiniGamesTimelineAnchorY),
            Offset(size.width * MiniGamesObservatoryAnchorX, size.height * MiniGamesObservatoryAnchorY),
        )
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = Color(0x99F6B73C),
                start = start,
                end = end,
                strokeWidth = size.minDimension * 0.012f,
            )
        }
        points.forEach { point ->
            drawCircle(
                color = Color(0xFFEAF3FF),
                radius = size.minDimension * 0.025f,
                center = point,
            )
            drawCircle(
                color = Color(0x7768E1D2),
                radius = size.minDimension * 0.06f,
                center = point,
                style = Stroke(width = size.minDimension * 0.006f),
            )
        }
    }
}

@Composable
private fun MiniGameInfoCallout(
    title: String,
    status: String,
    borderColor: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xCC07111A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.76f)),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (textAlign == TextAlign.Start) Alignment.Start else Alignment.End,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = status,
                color = Color(0xFFD6E7F7),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class MiniGameMenuButtonState {
    Available,
    Consumed,
    Loading,
    Disabled,
}

private data class MiniGameMenuButtonColors(
    val button: Color,
    val content: Color,
    val calloutBorder: Color,
)

private fun MiniGameMenuButtonState.colors(): MiniGameMenuButtonColors = when (this) {
    MiniGameMenuButtonState.Available -> MiniGameMenuButtonColors(
        button = Color(0xFFF6C75D),
        content = Color(0xFF221707),
        calloutBorder = Color(0xFFF6C75D),
    )

    MiniGameMenuButtonState.Consumed -> MiniGameMenuButtonColors(
        button = AuroraTeal,
        content = Color(0xFF06101D),
        calloutBorder = AuroraTeal,
    )

    MiniGameMenuButtonState.Loading -> MiniGameMenuButtonColors(
        button = Color(0xCC0A1524),
        content = Color.White.copy(alpha = 0.72f),
        calloutBorder = Color(0xFF6F849C),
    )

    MiniGameMenuButtonState.Disabled -> MiniGameMenuButtonColors(
        button = Color(0xCC0A1524),
        content = Color.White.copy(alpha = 0.72f),
        calloutBorder = Color(0xFF6F849C),
    )
}

private val MiniGameMenuButtonState.isInteractive: Boolean
    get() = this == MiniGameMenuButtonState.Available || this == MiniGameMenuButtonState.Consumed

// Coordinates mirror the center of the circles in mini-games-map.svg node-platforms.
private const val MiniGamesQuizAnchorX = 180f / 1000f
private const val MiniGamesQuizAnchorY = 1422f / 1778f
private const val MiniGamesMemoryAnchorX = 360f / 1000f
private const val MiniGamesMemoryAnchorY = 1084f / 1778f
private const val MiniGamesTimelineAnchorX = 620f / 1000f
private const val MiniGamesTimelineAnchorY = 711f / 1778f
private const val MiniGamesObservatoryAnchorX = 820f / 1000f
private const val MiniGamesObservatoryAnchorY = 356f / 1778f
private const val MiniGamesMapSvgAssetName = "mini-games-map.svg"
