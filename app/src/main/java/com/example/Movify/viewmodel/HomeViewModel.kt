package com.example.Movify.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Movify.model.Repository
import com.example.Movify.model.dataclasses.Genre
import com.example.Movify.model.dataclasses.Movie
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: Repository) : ViewModel() {

    var layoutManagerState: Parcelable? = null

    private val _genres = MutableLiveData<List<Genre>>()
    val genres: LiveData<List<Genre>> get() = _genres

    private val _popularMovies = MutableLiveData<List<Movie>>()
    val popularMovies: LiveData<List<Movie>> get() = _popularMovies

    private val _moviesByGenre = MutableLiveData<List<Movie>>()
    val moviesByGenre: LiveData<List<Movie>> get() = _moviesByGenre

    private val _topRatedMovies = MutableLiveData<List<Movie>>()
    val topRatedMovies: LiveData<List<Movie>> get() = _topRatedMovies

    private val _nowPlayingMovies = MutableLiveData<List<Movie>>()
    val nowPlayingMovies: LiveData<List<Movie>> get() = _nowPlayingMovies

    private val _selectedMovieDetails = MutableLiveData<Movie>()
    val selectedMovieDetails: LiveData<Movie> = _selectedMovieDetails

    init {
        fetchGenres()
        fetchPopularMovies()
        fetchTopRatedMovies()
        fetchNowPlayingMovies()
    }

    fun fetchGenres() {
        viewModelScope.launch {
            _genres.postValue(repository.getGenres())
        }
    }

    public fun fetchPopularMovies() {
        viewModelScope.launch {
            _popularMovies.postValue(repository.getPopularMovies())
        }
    }

    fun fetchMoviesByGenre(genreId: Int) {
        viewModelScope.launch {
            _moviesByGenre.postValue(repository.getMoviesByGenre(genreId))
        }
    }

    fun fetchTopRatedMovies() {
        viewModelScope.launch {
            _topRatedMovies.postValue(repository.getTopRatedMovies())
        }
    }

    fun fetchNowPlayingMovies() {
        viewModelScope.launch {
            _nowPlayingMovies.postValue(repository.getNowPlayingMovies())
        }
    }

    fun fetchMovieDetails(movieId: Int) {
        viewModelScope.launch {
            val response = repository.getMovieDetails(movieId)
            _selectedMovieDetails.value = response
        }
    }




}