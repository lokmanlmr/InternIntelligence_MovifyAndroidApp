package com.example.Movify.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Movify.model.Repository
import com.example.Movify.model.dataclasses.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeeAllViewModel(private var repository: Repository) : ViewModel() {

    private val _popularMovies = MutableLiveData<List<Movie>>()
    val popularMovies: LiveData<List<Movie>> get() = _popularMovies

    private val _topRatedMovies = MutableLiveData<List<Movie>>()
    val topRatedMovies: LiveData<List<Movie>> get() = _topRatedMovies

    // Recommended Movies
    private val _recommendedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val recommendedMovies: StateFlow<List<Movie>> = _recommendedMovies.asStateFlow()

    init {
        fetchPopularMovies()
        fetchTopRatedMovies()
    }

    fun fetchPopularMovies() {
        viewModelScope.launch {
            _popularMovies.postValue(repository.getPopularMovies())
        }
    }

    fun fetchTopRatedMovies() {
        viewModelScope.launch {
            _topRatedMovies.postValue(repository.getTopRatedMovies())
        }
    }

    fun fetchRecommendedMovies(movieId: Int) {
        viewModelScope.launch {
            try {
                val movies = repository.getRecommendationMovies(movieId)
                _recommendedMovies.value = movies
            } catch (e: Exception) {
                Log.e("MovieDetailViewModel", "Error fetching recommended movies for $movieId", e)
                _recommendedMovies.value = emptyList()
            }
        }
    }

}