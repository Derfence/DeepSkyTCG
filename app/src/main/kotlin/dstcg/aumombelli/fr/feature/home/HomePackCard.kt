package fr.aumombelli.dstcg.feature.home

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.NewContentIndicator
import fr.aumombelli.dstcg.ui.motion.MotionCard

@Composable
internal fun HomePackCard(
    enabled: Boolean,
    isBusy: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    miniGamesEnabled: Boolean = false,
    showMiniGamesNewIndicator: Boolean = false,
    onOpenMiniGamesMenu: () -> Unit = {},
    interactionTestTag: String? = null,
    modifier: Modifier = Modifier,
) {
    var flipStep by remember { mutableIntStateOf(0) }
    val showingMiniGamesBack = flipStep % 2 != 0
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 52.dp.toPx() }
    val rotationY by animateFloatAsState(
        targetValue = flipStep * 180f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "home-pack-card-flip",
    )
    val isBackSideRendered = isHomeCardMiniGamesFaceVisible(rotationY)

    LaunchedEffect(miniGamesEnabled) {
        if (!miniGamesEnabled) {
            flipStep = 0
        }
    }

    val swipeModifier = if (miniGamesEnabled) {
        Modifier.pointerInput(swipeThresholdPx, flipStep) {
            var totalDragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    totalDragX += dragAmount
                },
                onDragEnd = {
                    flipStep = nextHomeCardFlipStepForHorizontalDrag(
                        currentStep = flipStep,
                        dragX = totalDragX,
                        thresholdPx = swipeThresholdPx,
                    )
                    totalDragX = 0f
                },
                onDragCancel = { totalDragX = 0f },
            )
        }
    } else {
        Modifier
    }

    MotionCard(
        modifier = modifier
            .then(swipeModifier)
            .semantics(mergeDescendants = true) {}
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.78f
                this.rotationY = rotationY
                cameraDistance = 12f * density.density
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isBackSideRendered) {
                        this.rotationY = 180f
                    }
                },
        ) {
            if (isBackSideRendered && miniGamesEnabled) {
                HomeMiniGamesCardFace(
                    showNewIndicator = showMiniGamesNewIndicator,
                    onOpenMiniGamesMenu = onOpenMiniGamesMenu,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                HomePackCardFace(
                    enabled = enabled,
                    isBusy = isBusy,
                    title = title,
                    subtitle = subtitle,
                    onClick = onClick,
                    interactionTestTag = interactionTestTag,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (miniGamesEnabled) {
                IconButton(
                    onClick = { flipStep += 1 },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(44.dp)
                        .testTag("home-card-flip"),
                ) {
                    Surface(
                        color = Color(0xB30A1524),
                        contentColor = Color.White,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = "Retourner la carte",
                                modifier = Modifier.size(22.dp),
                            )
                            if (showMiniGamesNewIndicator && !showingMiniGamesBack) {
                                NewContentIndicator(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(15.dp)
                                        .testTag("home-mini-games-new-indicator"),
                                    iconSize = 12.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun nextHomeCardFlipStepForHorizontalDrag(
    currentStep: Int,
    dragX: Float,
    thresholdPx: Float,
): Int {
    if (kotlin.math.abs(dragX) < thresholdPx) {
        return currentStep
    }
    return currentStep + if (dragX < 0f) -1 else 1
}

internal fun isHomeCardMiniGamesFaceVisible(rotationY: Float): Boolean {
    val normalizedRotation = ((rotationY % 360f) + 360f) % 360f
    return normalizedRotation > 90f && normalizedRotation < 270f
}

@Composable
private fun HomePackCardFace(
    enabled: Boolean,
    isBusy: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    interactionTestTag: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        HomeCardSvgOrFallback(
            assetName = HomePackCardSvgAssetName,
            fallback = { LegacyHomePackCardBackground(modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.fillMaxSize(),
        )

        if (isBusy) {
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8E7F9),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (interactionTestTag != null) {
                        Modifier.testTag(interactionTestTag)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                ),
        )
    }
}

@Composable
private fun HomeMiniGamesCardFace(
    showNewIndicator: Boolean,
    onOpenMiniGamesMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag("home-mini-games-card")
            .clickable(onClick = onOpenMiniGamesMenu),
    ) {
        HomeCardSvgOrFallback(
            assetName = HomeMiniGamesCardSvgAssetName,
            fallback = { LegacyHomeMiniGamesCardBackground(modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(
                text = "Mini-jeux du jour",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "Joue chaque halte quotidienne pour accélérer la recharge.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8E7F9),
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onOpenMiniGamesMenu,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .testTag("home-mini-games-open-menu"),
            ) {
                Text("Menu des jeux")
            }
        }

        if (showNewIndicator) {
            NewContentIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .size(24.dp)
                    .testTag("home-mini-games-card-new-indicator"),
                iconSize = 18.dp,
            )
        }
    }
}

@Composable
private fun HomeCardSvgOrFallback(
    assetName: String,
    fallback: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val svgMarkup = remember(context, assetName) {
        runCatching {
            context.assets.open(assetName).bufferedReader().use { reader ->
                reader.readText()
            }
        }.getOrNull()
    }

    if (svgMarkup != null) {
        HomeCardSvgBackground(
            svgMarkup = svgMarkup,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier) {
            fallback()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HomeCardSvgBackground(
    svgMarkup: String,
    modifier: Modifier = Modifier,
) {
    val htmlDocument = remember(svgMarkup) {
        """
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
              html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: transparent;
              }
              svg {
                display: block;
                width: 100%;
                height: 100%;
              }
            </style>
          </head>
          <body>
            $svgMarkup
          </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { viewContext ->
            WebView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                overScrollMode = WebView.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isLongClickable = false
                settings.javaScriptEnabled = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                loadDataWithBaseURL(
                    "file:///android_asset/",
                    htmlDocument,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun LegacyHomePackCardBackground(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF112B49),
                    Color(0xFF091423),
                    Color(0xFF050910),
                ),
            ),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.18f),
                ),
            ),
            cornerRadius = CornerRadius(size.minDimension * 0.10f, size.minDimension * 0.10f),
        )
        drawRoundRect(
            color = Color(0x66F3D59F),
            cornerRadius = CornerRadius(size.minDimension * 0.10f, size.minDimension * 0.10f),
            style = Stroke(width = size.minDimension * 0.014f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x55E1F1FF),
                    Color.Transparent,
                ),
            ),
            radius = size.minDimension * 0.38f,
            center = Offset(size.width * 0.62f, size.height * 0.28f),
            blendMode = BlendMode.Screen,
        )
        drawCircle(
            color = Color(0x33FFC76A),
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.62f, size.height * 0.28f),
            blendMode = BlendMode.Screen,
        )

        val stars = listOf(
            Triple(0.18f, 0.16f, 0.010f),
            Triple(0.30f, 0.22f, 0.008f),
            Triple(0.42f, 0.14f, 0.012f),
            Triple(0.74f, 0.18f, 0.009f),
            Triple(0.82f, 0.30f, 0.007f),
            Triple(0.66f, 0.10f, 0.006f),
            Triple(0.24f, 0.34f, 0.007f),
        )
        stars.forEach { (x, y, radius) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.88f),
                radius = size.minDimension * radius,
                center = Offset(size.width * x, size.height * y),
            )
            drawCircle(
                color = Color(0x33FFD580),
                radius = size.minDimension * radius * 3.2f,
                center = Offset(size.width * x, size.height * y),
            )
        }

        drawLine(
            color = Color(0x55CFE2FF),
            start = Offset(size.width * 0.20f, size.height * 0.25f),
            end = Offset(size.width * 0.42f, size.height * 0.14f),
            strokeWidth = size.minDimension * 0.004f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0x55CFE2FF),
            start = Offset(size.width * 0.42f, size.height * 0.14f),
            end = Offset(size.width * 0.62f, size.height * 0.28f),
            strokeWidth = size.minDimension * 0.004f,
            cap = StrokeCap.Round,
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x33040911),
                    Color(0xCC040911),
                ),
                startY = size.height * 0.50f,
                endY = size.height,
            ),
            topLeft = Offset.Zero,
            size = size,
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x00000000),
                    Color(0x66112338),
                    Color(0xFF05080F),
                ),
            ),
            topLeft = Offset(0f, size.height * 0.58f),
            size = Size(size.width, size.height * 0.42f),
        )

        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.10f, size.height * 0.84f)
                lineTo(size.width * 0.24f, size.height * 0.70f)
                lineTo(size.width * 0.34f, size.height * 0.70f)
                lineTo(size.width * 0.34f, size.height * 0.57f)
                quadraticTo(
                    size.width * 0.42f,
                    size.height * 0.45f,
                    size.width * 0.52f,
                    size.height * 0.57f,
                )
                lineTo(size.width * 0.52f, size.height * 0.70f)
                lineTo(size.width * 0.66f, size.height * 0.70f)
                lineTo(size.width * 0.78f, size.height * 0.78f)
                lineTo(size.width * 0.90f, size.height * 0.78f)
                lineTo(size.width * 0.90f, size.height)
                lineTo(size.width * 0.10f, size.height)
                close()
            },
            color = Color(0xFF09111D),
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, size.height * 0.76f)
                quadraticTo(
                    size.width * 0.18f,
                    size.height * 0.72f,
                    size.width * 0.34f,
                    size.height * 0.75f,
                )
                quadraticTo(
                    size.width * 0.56f,
                    size.height * 0.78f,
                    size.width * 0.74f,
                    size.height * 0.73f,
                )
                quadraticTo(
                    size.width * 0.88f,
                    size.height * 0.70f,
                    size.width,
                    size.height * 0.74f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1F3652),
                    Color(0xFF0A121E),
                ),
            ),
        )
    }
}

@Composable
private fun LegacyHomeMiniGamesCardBackground(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF15131E),
                    Color(0xFF102A3D),
                    Color(0xFF07111A),
                ),
            ),
        )
        drawRoundRect(
            color = Color(0x66F6B73C),
            cornerRadius = CornerRadius(size.minDimension * 0.10f, size.minDimension * 0.10f),
            style = Stroke(width = size.minDimension * 0.014f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x5576E8FF), Color.Transparent),
            ),
            radius = size.minDimension * 0.44f,
            center = Offset(size.width * 0.56f, size.height * 0.34f),
            blendMode = BlendMode.Screen,
        )
        val stops = listOf(
            Offset(size.width * 0.22f, size.height * 0.72f),
            Offset(size.width * 0.40f, size.height * 0.58f),
            Offset(size.width * 0.58f, size.height * 0.42f),
            Offset(size.width * 0.76f, size.height * 0.28f),
        )
        stops.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = Color(0x88EAF3FF),
                start = start,
                end = end,
                strokeWidth = size.minDimension * 0.012f,
                cap = StrokeCap.Round,
            )
        }
        stops.forEachIndexed { index, stop ->
            drawCircle(
                color = Color(0xFFEAF3FF),
                radius = size.minDimension * (0.028f + index * 0.003f),
                center = stop,
            )
            drawCircle(
                color = Color(0x55F6B73C),
                radius = size.minDimension * 0.062f,
                center = stop,
                style = Stroke(width = size.minDimension * 0.006f),
            )
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xD0050810)),
                startY = size.height * 0.48f,
                endY = size.height,
            ),
            size = size,
        )
    }
}

private const val HomePackCardSvgAssetName: String = "carte_finale.svg"
private const val HomeMiniGamesCardSvgAssetName: String = "home-mini-games-card.svg"
