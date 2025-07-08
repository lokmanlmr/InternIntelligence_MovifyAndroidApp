package com.example.Movify.viewmodel

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Movify.model.Repository
import com.example.Movify.model.dataclasses.Movie
import com.example.Movify.model.dataclasses.Review
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MovieDetailViewModel(private val repository: Repository) : ViewModel() {

    val layoutManagerStates = mutableMapOf<Int, Parcelable?>()

    private val _movieDetails = MutableStateFlow<Movie?>(null)
    val movieDetails: StateFlow<Movie?> = _movieDetails.asStateFlow()

    private val _recommendedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val recommendedMovies: StateFlow<List<Movie>> = _recommendedMovies.asStateFlow()

    val reviews: StateFlow<List<Review>> = repository.currentMovieReviews

    private val _reviewSubmissionStatus = MutableLiveData<Boolean?>()
    val reviewSubmissionStatus: LiveData<Boolean?> get() = _reviewSubmissionStatus

    private var currentObservingMovieId: Int? = null

    private val _isMovieSaved = MutableStateFlow<Boolean?>(null)
    val isMovieSaved: StateFlow<Boolean?> = _isMovieSaved.asStateFlow()

    private val _movieSaveOperationStatus = MutableLiveData<Triple<Boolean, Boolean, String?>?>()
    val movieSaveOperationStatus: LiveData<Triple<Boolean, Boolean, String?>?> get() = _movieSaveOperationStatus

    fun loadMovieData(movieId: Int) {
        if (currentObservingMovieId != movieId) {
            _isMovieSaved.value = null
            currentObservingMovieId = movieId
            checkMovieSavedStatus(movieId)
        }
        fetchMovieDetails(movieId)
        fetchRecommendedMovies(movieId)
        fetchReviews(movieId)
    }

    private fun fetchMovieDetails(movieId: Int) {
        viewModelScope.launch {
            try {
                val details = repository.getMovieDetails(movieId)
                _movieDetails.value = details
                if (_isMovieSaved.value == null && currentObservingMovieId == movieId) {
                    checkMovieSavedStatus(movieId)
                }
            } catch (e: Exception) {
                _movieDetails.value = null
            }
        }
    }

    private fun fetchRecommendedMovies(movieId: Int) {
        viewModelScope.launch {
            try {
                val movies = repository.getRecommendationMovies(movieId)
                _recommendedMovies.value = movies
            } catch (e: Exception) {
                _recommendedMovies.value = emptyList()
            }
        }
    }

    fun fetchReviews(movieId: Int) {
        viewModelScope.launch {
            try {
                repository.getReviewsForMovie(movieId)
            } catch (e: Exception) {
                Log.e("MovieDetailViewModel", "Error in fetchReviews for $movieId", e)
            }
        }
    }

    fun addReview(movieId: Int, review: Review) {
        viewModelScope.launch {
            try {
                repository.addReview(movieId, review)
                _reviewSubmissionStatus.postValue(true)
            } catch (e: Exception) {
                Log.e("MovieDetailViewModel", "Error adding review for $movieId", e)
                _reviewSubmissionStatus.postValue(false)
            }
        }
    }

    fun clearReviewSubmissionStatus() {
        _reviewSubmissionStatus.value = null
    }

    private fun checkMovieSavedStatus(movieId: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _isMovieSaved.value = false
            return
        }
        viewModelScope.launch {
            repository.isMovieSaved(currentUser.uid, movieId)
                .catch { e ->
                    Log.e(
                        "MovieDetailViewModel",
                        "Error observing movie saved status for $movieId",
                        e
                    )
                    _isMovieSaved.value = false
                }
                .collectLatest { isSaved ->
                    Log.d("MovieDetailViewModel", "Movie $movieId saved status: $isSaved")
                    _isMovieSaved.value = isSaved
                }
        }
    }

    fun toggleSaveMovieStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentMovie = _movieDetails.value

        if (currentUser == null) {
            _movieSaveOperationStatus.postValue(Triple(true, false, "User not logged in."))
            return
        }
        if (currentMovie == null) {
            _movieSaveOperationStatus.postValue(Triple(true, false, "Movie details not loaded."))
            return
        }

        val movieId = currentMovie.id
        val movieTitle =
            currentMovie.title ?: "Unknown Title" // Should have title if movie is not null

        viewModelScope.launch {
            val isCurrentlySaved = _isMovieSaved.value ?: false

            if (isCurrentlySaved) {
                if (movieId != null) {
                    repository.unsaveMovieFromFirestore(currentUser.uid, movieId)
                        .collectLatest { success ->
                            if (success) {
                                _isMovieSaved.value = false
                                _movieSaveOperationStatus.postValue(
                                    Triple(
                                        false,
                                        true,
                                        "Movie unsaved."
                                    )
                                )
                            } else {
                                _movieSaveOperationStatus.postValue(
                                    Triple(
                                        false,
                                        false,
                                        "Failed to unsave movie."
                                    )
                                )
                            }
                        }
                }
            } else {
                Log.d("MovieDetailViewModel", "Attempting to save movie: $movieId")
                repository.getUserDetails(currentUser.uid) { userDetails ->
                    viewModelScope.launch {
                        if (userDetails == null) {
                            _movieSaveOperationStatus.postValue(
                                Triple(
                                    true,
                                    false,
                                    "Could not get user details to save movie."
                                )
                            )
                            Log.e(
                                "MovieDetailViewModel",
                                "Failed to get user details for user ${currentUser.uid}"
                            )
                            return@launch
                        }

                        if (movieId != null) {
                            repository.saveMovieToFirestore(
                                movieId = movieId,
                                movieTitle = movieTitle,
                                userId = currentUser.uid,
                                userEmail = userDetails.email
                                    ?: currentUser.email, // Fallback to auth email
                                username = userDetails.username
                            ).collectLatest { success ->
                                if (success) {
                                    _isMovieSaved.value = true
                                    _movieSaveOperationStatus.postValue(
                                        Triple(
                                            true,
                                            true,
                                            "Movie saved."
                                        )
                                    )
                                    Log.i(
                                        "MovieDetailViewModel",
                                        "Movie $movieId saved successfully by ${currentUser.uid}."
                                    )
                                } else {
                                    _movieSaveOperationStatus.postValue(
                                        Triple(
                                            true,
                                            false,
                                            "Failed to save movie."
                                        )
                                    )
                                    Log.e(
                                        "MovieDetailViewModel",
                                        "Failed to save movie $movieId for user ${currentUser.uid}."
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearMovieSaveOperationStatus() {
        _movieSaveOperationStatus.value = null
    }
}