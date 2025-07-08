package com.example.Movify.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Movify.model.Repository
import com.example.Movify.model.dataclasses.Movie
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider


class SavedViewModel(
    private val repository: Repository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _savedMoviesList = MutableLiveData<List<Movie>>()
    val savedMoviesList: LiveData<List<Movie>> get() = _savedMoviesList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadSavedMovies() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            _error.value = "Please log in to view saved movies."
            _savedMoviesList.value = emptyList()
            _isLoading.value = false
            Log.w("SavedViewModel", "User not authenticated.")
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val movieIds = repository.fetchAllSavedMovieIds(currentUser.uid)
                if (movieIds.isNotEmpty()) {
                    val movies = repository.getMoviesByIds(movieIds)
                    _savedMoviesList.value = movies
                } else {
                    _savedMoviesList.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load saved movies: ${e.localizedMessage}"
                _savedMoviesList.value = emptyList() // Clear list on error
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearError() {
        _error.value = null
    }

    fun onUserLoggedOut() {
        _savedMoviesList.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }
}

class SavedViewModelFactory(
    private val repository: Repository,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedViewModel(repository, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}