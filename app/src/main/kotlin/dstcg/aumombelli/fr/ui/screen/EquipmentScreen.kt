package fr.aumombelli.dstcg.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import fr.aumombelli.dstcg.feature.equipment.EquipmentScreen as EquipmentFeatureScreen
import fr.aumombelli.dstcg.ui.viewmodel.EquipmentUiState

@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onRefresh: () -> Unit,
    onActivateEquipment: (String) -> Unit,
    contentVisible: Boolean = true,
    onOnboardingActivationBoundsChanged: (Rect?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    EquipmentFeatureScreen(
        state = state,
        onRefresh = onRefresh,
        onActivateEquipment = onActivateEquipment,
        contentVisible = contentVisible,
        onOnboardingActivationBoundsChanged = onOnboardingActivationBoundsChanged,
        modifier = modifier,
    )
}
