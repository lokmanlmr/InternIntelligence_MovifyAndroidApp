package com.example.Movify.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.Movify.R
import com.example.Movify.databinding.FragmentMovieDetailBinding
import com.example.Movify.model.ListType
import com.example.Movify.model.Repository
import com.example.Movify.model.dataclasses.Review
import com.example.Movify.view.adapters.MovieAdapter
import com.example.Movify.view.adapters.ReviewAdapter
import com.example.Movify.viewmodel.MovieDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MovieDetailFragment : Fragment() {

    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private val navArgs: MovieDetailFragmentArgs by navArgs()

    private val viewModel: MovieDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MovieDetailViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MovieDetailViewModel(Repository()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var recommendedMoviesAdapter: MovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentMovieId = navArgs.movieId
        val currentMovieName = navArgs.title

        setupRecyclerViews(currentMovieId)
        setupReviewSubmission(currentMovieId)
        setupSaveMovieButton()
        observeViewModelData()

        binding.textSeeAll.setOnClickListener {
            findNavController().navigate(MovieDetailFragmentDirections.actionMovieDetailFragmentToSeeAllFragmentRecommended(
                listType = ListType.SIMILAR_MOVIES,
                movieId = currentMovieId,
                movieName = currentMovieName
            ))
        }

        viewModel.loadMovieData(currentMovieId)
    }

    private fun setupRecyclerViews(currentMovieId: Int) {
        binding.recyclerViewRecommended.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            viewModel.layoutManagerStates[currentMovieId]?.let { savedState ->
                layoutManager?.onRestoreInstanceState(savedState)
            }
            recommendedMoviesAdapter = MovieAdapter(emptyList()) { selectedRecommendedMovieId , movieName ->
                try {
                    val action = MovieDetailFragmentDirections.actionMovieDetailFragmentToSelf(selectedRecommendedMovieId,movieName)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Navigation to recommended movie failed.", Toast.LENGTH_SHORT).show()
                }
            }
            adapter = recommendedMoviesAdapter
        }

        reviewAdapter = ReviewAdapter()
        binding.recyclerViewReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }
    }

    private fun setupReviewSubmission(currentMovieId: Int) {
        binding.ratingBarReview.setOnRatingBarChangeListener { _, rating, _ ->
            binding.textRatingValue.text = String.format(Locale.getDefault(), "%.1f", rating)
        }

        binding.buttonAddReview.setOnClickListener {
            val reviewContent = binding.editTextReview.text.toString().trim()
            val reviewRating = binding.ratingBarReview.rating.toDouble()
            val user = FirebaseAuth.getInstance().currentUser

            if (reviewContent.isEmpty()) {
                Toast.makeText(requireContext(), "Review content cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (reviewRating <= 0) {
                Toast.makeText(requireContext(), "Please provide a rating.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (user == null) {
                Toast.makeText(requireContext(), "You need to be logged in to perform this action.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username") ?: user.displayName ?: user.email ?: "anonymous_user"
                    val newReview = Review(
                        id = UUID.randomUUID().toString(),
                        author = username,
                        content = reviewContent,
                        rating = reviewRating,
                        date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    viewModel.addReview(currentMovieId, newReview)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to fetch username for review.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupSaveMovieButton() {
        binding.buttonSaveMovie.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "You need to be logged in to perform this action.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.toggleSaveMovieStatus()
        }
    }

    private fun observeViewModelData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movieDetails.collectLatest { movie ->
                movie?.let {
                    binding.movieTitle.text = it.title
                    binding.movieReleaseDate.text = String.format("Release Date: %s", it.releaseDate ?: "N/A")
                    binding.movieRating.text = String.format(Locale.getDefault(), "Rating: %.1f / 10", it.voteAverage ?: 0.0)
                    binding.movieDescription.text = it.overview ?: ""

                    val imageBaseUrl = "https://image.tmdb.org/t/p/w500"
                    Glide.with(this@MovieDetailFragment)
                        .load(imageBaseUrl + it.backdropPath)
                        .placeholder(R.drawable.error)
                        .error(R.drawable.error)
                        .into(binding.movieBackdropImage)
                    Glide.with(this@MovieDetailFragment)
                        .load(imageBaseUrl + it.posterPath)
                        .placeholder(R.drawable.error)
                        .error(R.drawable.error)
                        .into(binding.moviePosterImage)
                } ?: run {
                    binding.movieTitle.text = "Movie details not found."
                    binding.movieReleaseDate.text = ""
                    binding.movieRating.text = ""
                    binding.movieDescription.text = ""
                    binding.movieBackdropImage.setImageResource(R.drawable.error)
                    binding.moviePosterImage.setImageResource(R.drawable.error)

                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendedMovies.collectLatest { movies ->
                recommendedMoviesAdapter.updateMovies(movies)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reviews.collectLatest { reviews ->
                reviewAdapter.submitList(reviews)
                Log.d("MovieDetailFragment", String.format("Reviews updated in UI. Count: %d", reviews.size))
            }
        }

        viewModel.reviewSubmissionStatus.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Toast.makeText(requireContext(), "Review posted successfully!", Toast.LENGTH_SHORT).show()
                    binding.editTextReview.text.clear()
                    binding.ratingBarReview.rating = 0f
                    binding.textRatingValue.text = "0.0"
                } else {
                    Toast.makeText(requireContext(), "Failed to post review. Please try again.", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearReviewSubmissionStatus()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isMovieSaved.collectLatest { isSaved ->
                when (isSaved) {
                    true -> {
                        binding.buttonSaveMovie.setImageResource(R.drawable.star_s)
                        binding.buttonSaveMovie.contentDescription = "Unsave this movie"
                    }
                    false -> {
                        binding.buttonSaveMovie.setImageResource(R.drawable.star_ns)
                        binding.buttonSaveMovie.contentDescription = "Save this movie"
                    }
                    null -> {
                        binding.buttonSaveMovie.setImageResource(R.drawable.star_ns)
                        binding.buttonSaveMovie.contentDescription = "Save this movie"
                    }
                }
            }
        }

        viewModel.movieSaveOperationStatus.observe(viewLifecycleOwner) { result ->
            result?.let { (wasAttemptToSave, wasSuccessful, _) ->
                val message = when {
                    wasAttemptToSave && wasSuccessful -> "Movie saved!"
                    !wasAttemptToSave && wasSuccessful -> "Movie removed from saved list."
                    wasAttemptToSave && !wasSuccessful -> "Failed to save movie."
                    !wasAttemptToSave && !wasSuccessful -> "Failed to remove movie."
                    else -> null
                }
                message?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearMovieSaveOperationStatus()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.recyclerViewRecommended.layoutManager?.let { layoutManager ->
            viewModel.layoutManagerStates[navArgs.movieId] = layoutManager.onSaveInstanceState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.recyclerViewRecommended.adapter = null
        binding.recyclerViewReviews.adapter = null
        _binding = null
    }
}