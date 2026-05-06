package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerBlockingModal
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalPage
import fr.aumombelli.dstcg.model.CraftingToolWalkthroughPageContent
import fr.aumombelli.dstcg.model.NewPlayerOnboardingContent

@Composable
internal fun CraftingOnboardingToolsWalkthrough(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = NewPlayerOnboardingContent.craftingToolWalkthroughPages
    NewPlayerBlockingModal(
        testTag = "new-player-modal-crafting-tools",
        pages = pages.map { page ->
            NewPlayerBlockingModalPage(
                title = page.title,
                message = page.message,
            )
        },
        finishButtonLabel = "Terminer",
        onFinished = onCompleted,
        modifier = modifier,
        pageContent = { pageIndex ->
            CraftingToolCostPanel(
                page = pages[pageIndex],
                pageIndex = pageIndex,
            )
        },
    )
}

@Composable
private fun CraftingToolCostPanel(
    page: CraftingToolWalkthroughPageContent,
    pageIndex: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xCC07111B),
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(Color(0x88F5D58F), Color(0x3368E1D2)),
            ),
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("new-player-modal-crafting-tools-costs-$pageIndex"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Coûts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            page.costs.forEachIndexed { index, cost ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new-player-modal-crafting-tools-cost-$pageIndex-$index"),
                ) {
                    Text(
                        text = cost.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE1ECF8),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = cost.cost,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF5D58F),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
