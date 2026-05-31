package fr.aumombelli.dstcg.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

internal data class NewPlayerBlockingModalPage(
    val title: String,
    val message: String,
)

@Composable
internal fun NewPlayerBlockingModal(
    testTag: String,
    pages: List<NewPlayerBlockingModalPage>,
    finishButtonLabel: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    decorativeBottomAvoidanceHeight: BoxWithConstraintsScope.() -> Dp = { 0.dp },
    decorativeBottomAvoidanceGap: Dp = 12.dp,
    decorativeOverlay: @Composable BoxScope.() -> Unit = {},
    heightAwarePageContent: (@Composable (Int, Dp) -> Unit)? = null,
    pageContent: @Composable (Int) -> Unit = {},
) {
    if (pages.isEmpty()) return

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 56.dp.toPx() }
    var enterAnimationStarted by remember { mutableStateOf(false) }
    val backdropAlpha by animateFloatAsState(
        targetValue = if (enterAnimationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "new-player-modal-backdrop-alpha",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (enterAnimationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "new-player-modal-card-alpha",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (enterAnimationStarted) 1f else 0.94f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "new-player-modal-card-scale",
    )
    val cardTranslationY by animateFloatAsState(
        targetValue = if (enterAnimationStarted) 0f else with(density) { 24.dp.toPx() },
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "new-player-modal-card-translation-y",
    )

    LaunchedEffect(Unit) {
        enterAnimationStarted = true
    }

    val moveToPage: (Int) -> Unit = { targetPage ->
        scope.launch {
            pagerState.scrollToPage(targetPage.coerceIn(0, pages.lastIndex))
        }
    }

    Dialog(
        onDismissRequest = {
            if (pagerState.currentPage > 0) {
                moveToPage(pagerState.currentPage - 1)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A1220).copy(alpha = 0.9f * backdropAlpha))
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical,
                    ),
                )
                .padding(start = 18.dp, top = 24.dp, end = 18.dp, bottom = 40.dp)
                .testTag(testTag),
        ) {
            val modalMaxHeight = this@BoxWithConstraints.maxHeight
            val bottomAvoidanceHeight = decorativeBottomAvoidanceHeight()
            val modalMaxHeightPx = with(density) { modalMaxHeight.toPx() }
            val bottomAvoidanceHeightPx = with(density) { bottomAvoidanceHeight.toPx() }
            val bottomAvoidanceGapPx = with(density) { decorativeBottomAvoidanceGap.toPx() }
            var cardHeightPx by remember { mutableStateOf(0) }
            val avoidanceTranslationY = newPlayerModalCardVerticalShiftPx(
                modalHeightPx = modalMaxHeightPx,
                cardHeightPx = cardHeightPx.toFloat(),
                bottomAvoidanceHeightPx = bottomAvoidanceHeightPx,
                gapPx = bottomAvoidanceGapPx,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10233B)),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .onSizeChanged { size -> cardHeightPx = size.height }
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                        translationY = cardTranslationY + avoidanceTranslationY
                    }
                    .testTag("new-player-modal-card")
                    .fillMaxWidth()
                    .heightIn(max = modalMaxHeight),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF15314F), Color(0xFF0C1728)),
                            ),
                        )
                        .heightIn(max = modalMaxHeight)
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                ) {
                    val pagerModifier = if (pages.size > 1) {
                        Modifier.weight(1f, fill = heightAwarePageContent != null)
                    } else {
                        Modifier
                    }
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = pagerModifier
                            .fillMaxWidth()
                            .pointerInput(pages.size, pagerState.currentPage) {
                                if (pages.size <= 1) return@pointerInput

                                var totalDragX = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        totalDragX += dragAmount
                                        change.consume()
                                    },
                                    onDragEnd = {
                                        when {
                                            totalDragX <= -swipeThresholdPx &&
                                                pagerState.currentPage < pages.lastIndex ->
                                                moveToPage(pagerState.currentPage + 1)

                                            totalDragX >= swipeThresholdPx &&
                                                pagerState.currentPage > 0 ->
                                                moveToPage(pagerState.currentPage - 1)
                                        }
                                        totalDragX = 0f
                                    },
                                    onDragCancel = {
                                        totalDragX = 0f
                                    },
                                )
                            },
                    ) { pageIndex ->
                        val page = pages[pageIndex]
                        if (heightAwarePageContent != null) {
                            HeightAwareModalPage(
                                page = page,
                                pageIndex = pageIndex,
                                pageContent = heightAwarePageContent,
                            )
                        } else {
                            ScrollableModalPage(
                                page = page,
                                pageIndex = pageIndex,
                                pageContent = pageContent,
                            )
                        }
                    }

                    if (pages.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            repeat(pages.size) { pageIndex ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(width = if (pageIndex == pagerState.currentPage) 28.dp else 10.dp, height = 10.dp)
                                        .background(
                                            color = if (pageIndex == pagerState.currentPage) {
                                                Color(0xFFF5D58F)
                                            } else {
                                                Color(0xFF5A718C)
                                            },
                                            shape = RoundedCornerShape(99.dp),
                                        ),
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (pages.size > 1 && pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    moveToPage(pagerState.currentPage - 1)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new-player-modal-previous"),
                            ) {
                                Text("Précédent")
                            }
                        }

                        if (pagerState.currentPage < pages.lastIndex) {
                            Button(
                                onClick = {
                                    moveToPage(pagerState.currentPage + 1)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new-player-modal-next"),
                            ) {
                                Text("Suivant")
                            }
                        } else {
                            Button(
                                onClick = onFinished,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new-player-modal-finish"),
                            ) {
                                Text(finishButtonLabel)
                            }
                        }
                    }
                }
            }
            decorativeOverlay()
        }
    }
}

@Composable
private fun ScrollableModalPage(
    page: NewPlayerBlockingModalPage,
    pageIndex: Int,
    pageContent: @Composable (Int) -> Unit,
) {
    val pageScrollState = rememberScrollState()
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(pageScrollState)
            .testTag("new-player-modal-page-$pageIndex"),
    ) {
        ModalPageText(page)
        pageContent(pageIndex)
    }
}

@Composable
private fun HeightAwareModalPage(
    page: NewPlayerBlockingModalPage,
    pageIndex: Int,
    pageContent: @Composable (Int, Dp) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("new-player-modal-page-$pageIndex"),
    ) {
        ModalPageText(page)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
        ) {
            pageContent(pageIndex, maxHeight)
        }
    }
}

@Composable
private fun ModalPageText(
    page: NewPlayerBlockingModalPage,
) {
    Text(
        text = page.title,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = page.message,
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFFE1ECF8),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

internal fun newPlayerModalCardVerticalShiftPx(
    modalHeightPx: Float,
    cardHeightPx: Float,
    bottomAvoidanceHeightPx: Float,
    gapPx: Float,
): Float {
    if (
        modalHeightPx <= 0f ||
        cardHeightPx <= 0f ||
        bottomAvoidanceHeightPx <= 0f
    ) {
        return 0f
    }
    val maxCardBottomPx = modalHeightPx - bottomAvoidanceHeightPx - gapPx
    if (maxCardBottomPx < cardHeightPx) return 0f

    val centeredCardBottomPx = (modalHeightPx + cardHeightPx) / 2f
    return (maxCardBottomPx - centeredCardBottomPx).coerceAtMost(0f)
}
