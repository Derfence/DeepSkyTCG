package fr.aumombelli.dstcg.feature.start

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun StartScreen(
    state: StartUiState,
    onBegin: () -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    contentVisible: Boolean = true,
    onCardTopChanged: (Float) -> Unit = {},
) {
    var resetConfirmationVisible by remember { mutableStateOf(false) }
    var aboutSheetVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "start-card-alpha",
    )
    val backgroundBrush = if (showBackground) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF08101D),
                Color(0xFF12243F),
                Color(0xFF1A3052),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
            ),
        )
    }

    LaunchedEffect(contentVisible) {
        if (!contentVisible) {
            resetConfirmationVisible = false
            aboutSheetVisible = false
        }
    }

    BackHandler(enabled = resetConfirmationVisible || aboutSheetVisible) {
        if (resetConfirmationVisible) {
            resetConfirmationVisible = false
        } else {
            aboutSheetVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
    ) {
        StartCard(
            state = state,
            cardAlpha = contentAlpha,
            onBegin = onBegin,
            onResetProgress = {
                aboutSheetVisible = false
                resetConfirmationVisible = true
            },
            onCardTopChanged = onCardTopChanged,
        )

        StartFooter(
            contentAlpha = contentAlpha,
            onOpenAbout = {
                if (contentVisible) {
                    aboutSheetVisible = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        StartAboutSheet(
            visible = aboutSheetVisible && contentVisible,
            onDismiss = { aboutSheetVisible = false },
        )

        if (resetConfirmationVisible && contentVisible) {
            StartResetConfirmationDialog(
                onConfirm = {
                    resetConfirmationVisible = false
                    onResetProgress()
                },
                onCancel = { resetConfirmationVisible = false },
            )
        }
    }
}
