package com.example.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MediaRepository
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GalleryFilter {
    ALL,
    IMAGES,
    VIDEOS,
    FILMS
}

data class GalleryUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedFilter: GalleryFilter = GalleryFilter.ALL,
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val isLoading: Boolean = false
)

class GalleryViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            mediaRepository.allMedia.collect { all ->
                _uiState.update { it.copy(mediaItems = all) }
            }
        }
    }

    fun setFilter(filter: GalleryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun deleteMedia(id: String) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(id)
        }
    }
}
