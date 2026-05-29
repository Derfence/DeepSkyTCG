package fr.aumombelli.dstcg.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.CollectionGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.model.markLibraryNoveltyPresented
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val sections: List<LibrarySection> = emptyList(),
    val onboardingVariantWalkthroughPages: List<LibraryOnboardingVariantWalkthroughPage> = emptyList(),
    val errorMessage: String? = null,
)

class LibraryViewModel(
    private val catalogRepository: CatalogGateway,
    private val collectionRepository: CollectionGateway,
    private val progressRepository: ProgressGateway? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val extensions = catalogRepository.loadExtensions()
                val cards = catalogRepository.loadCards()
                val variantProfiles = catalogRepository.loadVariantProfiles()
                val progress = progressRepository
                    ?.loadProgress()
                    ?.requireUsableProgress()
                    ?.progress
                val collection = progress?.collection ?: collectionRepository.loadCollection()
                LoadedLibraryContent(
                    sections = buildLibrarySections(
                        extensions = extensions,
                        cards = cards,
                        variantProfiles = variantProfiles,
                        collection = collection,
                        newCardIds = progress?.libraryCardNoveltyState?.newCardIds.orEmpty(),
                    ),
                    onboardingVariantWalkthroughPages = buildLibraryOnboardingVariantWalkthroughPages(
                        extensions = extensions,
                        cards = cards,
                        variantProfiles = variantProfiles,
                    ),
                    shouldClearPresentedNovelty = progressRepository != null &&
                        (
                            progress?.homeMenuNoveltyState?.library == true ||
                                progress?.libraryCardNoveltyState?.newCardIds?.isNotEmpty() == true
                        ),
                )
            }.onSuccess { content ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    sections = content.sections,
                    onboardingVariantWalkthroughPages = content.onboardingVariantWalkthroughPages,
                )
                if (content.shouldClearPresentedNovelty) {
                    viewModelScope.launch {
                        runCatching {
                            progressRepository?.updateProgress { progress ->
                                progress.markLibraryNoveltyPresented()
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Impossible de charger la bibliothèque.",
                )
            }
        }
    }
}

private data class LoadedLibraryContent(
    val sections: List<LibrarySection>,
    val onboardingVariantWalkthroughPages: List<LibraryOnboardingVariantWalkthroughPage>,
    val shouldClearPresentedNovelty: Boolean,
)
