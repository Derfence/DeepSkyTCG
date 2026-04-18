package fr.aumombelli.dstcg.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A1220).copy(alpha = 0.9f * backdropAlpha))
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .testTag(testTag),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10233B)),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                        translationY = cardTranslationY
                    }
                    .fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF15314F), Color(0xFF0C1728)),
                            ),
                        )
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new-player-modal-page-$pageIndex"),
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
                            pageContent(pageIndex)
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
        }
    }
}
