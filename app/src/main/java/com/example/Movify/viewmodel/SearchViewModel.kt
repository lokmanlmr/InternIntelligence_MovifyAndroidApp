package com.example.Movify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Movify.model.Repository
import com.example.Movify.model.dataclasses.Movie
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: Repository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<Movie>>()
    val searchResults: LiveData<List<Movie>> get() = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> get() = _errorMessage

    private var searchJob: Job? = null

    fun searchMovies(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            _errorMessage.value = null
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        searchJob = viewModelScope.launch {
            delay(500)
            val movies = repository.searchMovies(query)
            _searchResults.value = movies
            _isLoading.value = false
            if (movies.isEmpty()) {
                _errorMessage.value = "No results found for '$query'."
            }
        }
    }
}

class SearchViewModelFactory(private val repository: Repository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}