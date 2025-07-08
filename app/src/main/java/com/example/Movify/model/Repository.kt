package com.example.Movify.model

import android.util.Log
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.example.Movify.model.dataclasses.Genre
import com.example.Movify.model.dataclasses.Movie
import com.example.Movify.model.dataclasses.Review
import com.example.Movify.model.dataclasses.UserDetails
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.HttpException
import java.io.IOException

class Repository {

    // Firebase Instances and Collection References
    private val db = FirebaseFirestore.getInstance()
    private val reviewsCollection = db.collection("reviews")
    private val savedMoviesCollection = db.collection("saved_movies")
    private val usersCollection = db.collection("users")


    // Fetching Genres from TMDB
    suspend fun getGenres(): List<Genre> {
        return try {
            val response = RetrofitClient.tmdbApiService.getGenres(RetrofitClient.API_KEY)
            if (response.isSuccessful) {
                response.body()?.genres ?: emptyList<Genre>().also {
                    Log.d("Repository", "Genres fetched: ${it.size}")
                }
            } else {
                Log.e("Repository", "API Error fetching genres: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: IOException) {
            Log.e("Repository", "Network error fetching genres: ${e.message}", e)
            emptyList() // Return empty list on network failure
        } catch (e: HttpException) {
            Log.e("Repository", "HTTP error fetching genres: ${e.message}", e)
            emptyList() // Return empty list on HTTP error
        } catch (e: Exception) {
            Log.e("Repository", "Generic error fetching genres: ${e.message}", e)
            emptyList() // Fallback for other unexpected errors
        }
    }

    suspend fun getPopularMovies(): List<Movie> {
        return try {
            val response = RetrofitClient.tmdbApiService.getPopularMovies(RetrofitClient.API_KEY)
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                Log.e("Repository", "API Error fetching popular movies: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: IOException) {
            Log.e("Repository", "Network error fetching popular movies: ${e.message}", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e("Repository", "HTTP error fetching popular movies: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Generic error fetching popular movies: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMoviesByGenre(genreId: Int): List<Movie> {
        return try {
            // Assuming RetrofitClient.tmdbApiService.discoverMoviesByGenre
            // returns an object that directly has a 'results' property (e.g., YourMovieResponseObject)
            val response = RetrofitClient.tmdbApiService.discoverMoviesByGenre(
                apiKey = RetrofitClient.API_KEY,
                genreIds = genreId.toString()
            )
            response.results ?: emptyList()
        } catch (e: IOException) {
            Log.e("Repository", "Network error fetching movies by genre $genreId: ${e.message}", e)
            emptyList()
        } catch (e: HttpException) {
            // This catch block is useful if Retrofit throws an HttpException
            // for non-2xx responses even when the return type isn't Response<T>.
            // This can happen depending on how your errors are processed by Retrofit/OkHttp.
            Log.e("Repository", "HTTP error fetching movies by genre $genreId: ${e.message()}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Generic error fetching movies by genre $genreId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTopRatedMovies(): List<Movie> {
        return try {
            // Assuming RetrofitClient.tmdbApiService.getTopRatedMovies
            // returns an object that directly has a 'results' property
            val response = RetrofitClient.tmdbApiService.getTopRatedMovies(RetrofitClient.API_KEY)
            response.results ?: emptyList()
        } catch (e: IOException) {
            Log.e("Repository", "Network error fetching top rated movies: ${e.message}", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e("Repository", "HTTP error fetching top rated movies: ${e.message()}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Generic error fetching top rated movies: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getNowPlayingMovies(): List<Movie> {
        return try {
            val response = RetrofitClient.tmdbApiService.getNowPlayingMovies(RetrofitClient.API_KEY)
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                Log.e(
                    "Repository",
                    "API Error fetching now playing movies: ${response.code()} - ${response.message()}"
                )
                emptyList()
            }
        } catch (e: IOException) {
            Log.e("Repository", "Network error fetching now playing movies: ${e.message}", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e("Repository", "HTTP error fetching now playing movies: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Generic error fetching now playing movies: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMovieDetails(movieId: Int): Movie {
        return RetrofitClient.tmdbApiService.getMovieDetails(movieId, RetrofitClient.API_KEY)
    }

    suspend fun getRecommendationMovies(movieId: Int): List<Movie> {
        val response =
            RetrofitClient.tmdbApiService.getRecommendationMovies(movieId, RetrofitClient.API_KEY)
        return response.results ?: emptyList()
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<Movie> {
        val response = RetrofitClient.tmdbApiService.searchMovies(
            query = query,
            language = "en-US",
            page = page,
            apiKey = RetrofitClient.API_KEY
        )
        return if (response.isSuccessful) {
            response.body()?.results ?: emptyList()
        } else {
            Log.e("Repository", "Error searching movies: ${response.errorBody()?.string()}")
            emptyList()
        }
    }

    // --- Review Management ---
    private val _currentMovieReviews = MutableStateFlow<List<Review>>(emptyList())
    val currentMovieReviews: StateFlow<List<Review>> = _currentMovieReviews.asStateFlow()

    suspend fun getReviewsForMovie(movieId: Int) {
        Log.d("Repository", "Fetching reviews for movie ID: $movieId from Firestore.")
        try {
            val snapshot = reviewsCollection
                .whereEqualTo("movieId", movieId.toLong())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { document ->
                document.toObject(Review::class.java)?.copy(id = document.id)
            }
            _currentMovieReviews.value = reviews
            Log.d("Repository", "Fetched ${reviews.size} reviews for movie ID: $movieId.")
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching reviews for movie ID $movieId: ${e.message}", e)
            _currentMovieReviews.value = emptyList()
        }
    }

    suspend fun addReview(movieId: Int, review: Review) {
        Log.d("Repository", "Adding review for movie ID: $movieId to Firestore.")
        try {
            val reviewData = hashMapOf(
                "author" to review.author,
                "content" to review.content,
                "rating" to review.rating,
                "date" to (review.date.ifEmpty {
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                }),
                "movieId" to movieId.toLong()
            )
            reviewsCollection.add(reviewData).await()
            Log.d("Repository", "Review added. Refreshing reviews for movie ID: $movieId.")
            getReviewsForMovie(movieId)
        } catch (e: Exception) {
            Log.e("Repository", "Error adding review for movie ID $movieId: ${e.message}", e)
            throw e
        }
    }


    fun getUserDetails(userId: String, callback: (UserDetails?) -> Unit) {
        if (userId.isBlank()) {
            Log.w("Repository", "getUserDetails called with blank userId.")
            callback(null)
            return
        }
        usersCollection.document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username =
                        document.getString("username") ?: "Anonymous"
                    val email = document.getString("email")
                    Log.d(
                        "Repository",
                        "User details fetched for $userId: username='${username}', email='${email}'"
                    )
                    callback(UserDetails(username, email))
                } else {
                    Log.w("Repository", "No user document found for ID: $userId")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Repository", "Error fetching user details for $userId", exception)
                callback(null)
            }
    }

    fun isMovieSaved(userId: String, movieId: Int): Flow<Boolean> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(false).isSuccess
                close()
                return@callbackFlow
            }
            val query = savedMoviesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("movieId", movieId.toLong())
                .limit(1)

            val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(
                        "Repository",
                        "Listen failed for isMovieSaved. User: $userId, Movie: $movieId",
                        error
                    )
                    trySend(false).isSuccess
                    return@addSnapshotListener
                }
                val isSaved = snapshot != null && !snapshot.isEmpty
                Log.d(
                    "Repository",
                    "isMovieSaved check. User: $userId, Movie: $movieId, Saved: $isSaved. Docs: ${snapshot?.size()}"
                )
                trySend(isSaved).isSuccess
            }
            awaitClose {
                Log.d(
                    "Repository",
                    "Cancelling isMovieSaved listener. User: $userId, Movie: $movieId"
                )
                listenerRegistration.remove()
            }
        }

    fun saveMovieToFirestore(
        movieId: Int,
        movieTitle: String,
        userId: String,
        userEmail: String?,
        username: String
    ): Flow<Boolean> = flow {
        if (userId.isBlank()) {
            Log.w("Repository", "Save attempt with blank userId for movie $movieId")
            emit(false)
            return@flow
        }
        try {
            val existingQuery = savedMoviesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("movieId", movieId.toLong())
                .limit(1)
                .get()
                .await()

            if (!existingQuery.isEmpty) {
                Log.i("Repository", "Movie $movieId already saved by user $userId. Emitting true.")
                emit(true)
                return@flow
            }

            val data = hashMapOf(
                "userId" to userId,
                "movieId" to movieId.toLong(),
                "movieTitle" to movieTitle,
                "username" to username,
                "email" to userEmail,
                "savedAt" to FieldValue.serverTimestamp()
            )
            savedMoviesCollection.add(data).await()
            Log.i("Repository", "Movie $movieId saved successfully for user $userId.")
            emit(true)
        } catch (e: Exception) {
            Log.e("Repository", "Error saving movie $movieId for user $userId: ${e.message}", e)
            emit(false)
        }
    }


    fun unsaveMovieFromFirestore(userId: String, movieId: Int): Flow<Boolean> = flow {
        if (userId.isBlank()) {
            Log.w("Repository", "Unsave attempt with blank userId for movie $movieId")
            emit(false)
            return@flow
        }
        try {
            val querySnapshot = savedMoviesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("movieId", movieId.toLong())
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.w(
                    "Repository",
                    "Movie $movieId not found for user $userId to unsave. Emitting true (already in desired state)."
                )
                emit(true)
                return@flow
            }

            val batch = db.batch()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
            Log.i("Repository", "Movie $movieId unsaved successfully for user $userId.")
            emit(true)
        } catch (e: Exception) {
            Log.e("Repository", "Error unsaving movie $movieId for user $userId: ${e.message}", e)
            emit(false)
        }
    }

    suspend fun fetchAllSavedMovieIds(userUid: String): List<Int> {
        if (userUid.isBlank()) {
            Log.e("Repository", "User UID is blank for fetchAllSavedMovieIds.")
            return emptyList()
        }
        return try {
            Log.d("Repository", "Fetching saved movie IDs for user: $userUid")
            val snapshot = savedMoviesCollection
                .whereEqualTo("userId", userUid)
                .get()
                .await()

            val movieIds = snapshot.documents.mapNotNull { document ->
                document.getLong("movieId")?.toInt().also {
                }
            }
            Log.d("Repository", "Found ${movieIds.size} saved movie IDs for user $userUid.")
            movieIds
        } catch (e: FirebaseFirestoreException) {
            Log.e(
                "Repository",
                "Firestore error fetching saved movie IDs for $userUid: ${e.code}, Msg: ${e.message}",
                e
            )
            emptyList()
        } catch (e: Exception) {
            Log.e(
                "Repository",
                "Generic error fetching saved movie IDs for $userUid: ${e.message}",
                e
            )
            emptyList()
        }
    }

    suspend fun getMoviesByIds(movieIds: List<Int>, appendToResponse: String? = null): List<Movie> {
        if (movieIds.isEmpty()) return emptyList()
        val movies = mutableListOf<Movie>()
        movieIds.forEach { id ->
            try {
                val movie = RetrofitClient.tmdbApiService.getMovieDetails(
                    movieId = id,
                    apiKey = RetrofitClient.API_KEY,
                    appendToResponse = appendToResponse
                )
                movies.add(movie)
            } catch (e: IOException) { // Specifically catch IOException
                Log.e("Repository", "Network error fetching movie details for ID $id: ${e.message}")
                // Optionally, you could decide to continue and try fetching other movies,
                // or return the partially fetched list, or an empty list if any fails.
                // For simplicity here, we log and continue (the movie won't be added).
            } catch (e: HttpException) {
                Log.e("Repository", "HTTP error fetching movie details for ID $id: ${e.code()} - ${e.message()}")
            } catch (e: Exception) { // Catch other potential exceptions
                Log.e("Repository", "Unknown error fetching movie details for ID $id: ${e.message}")
            }
        }
        return movies
    }
}