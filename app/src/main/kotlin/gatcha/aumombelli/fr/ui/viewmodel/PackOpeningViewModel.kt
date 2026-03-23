package fr.aumombelli.gatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.model.DrawPackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PackOpeningUiState(
    val packResult: DrawPackResponse? = null,
)

class PackOpeningViewModel(
    packRepository: PackGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PackOpeningUiState())
    val uiState: StateFlow<PackOpeningUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            packRepository.currentPackResult().collect { packResult ->
                _uiState.value = PackOpeningUiState(packResult = packResult)
            }
        }
    }
}
