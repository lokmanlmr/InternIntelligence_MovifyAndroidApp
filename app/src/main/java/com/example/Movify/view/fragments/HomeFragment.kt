package com.example.Movify.view.fragments

import ActivityCommsViewModel
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.Movify.databinding.FragmentHomeBinding
import com.example.Movify.model.ListType
import com.example.Movify.model.Repository
import com.example.Movify.view.adapters.GenreAdapter
import com.example.Movify.view.adapters.MovieAdapter
import com.example.Movify.view.adapters.MovieSliderAdapter
import com.example.Movify.viewmodel.HomeViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Adapters
    private lateinit var movieSliderAdapter: MovieSliderAdapter
    private lateinit var genreAdapter: GenreAdapter
    private lateinit var moviesByGenreAdapter: MovieAdapter
    private lateinit var topRatedAdapter: MovieAdapter
    private lateinit var popularMoviesAdapter: MovieAdapter

    private val activityCommsViewModel: ActivityCommsViewModel by activityViewModels()

    // Slider Handler
    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = object : Runnable {
        override fun run() {
            if (!isAdded || _binding == null) return // Prevent crash if fragment is detached

            val vp = binding.movieSliderViewPager
            val itemCount = movieSliderAdapter.itemCount
            if (itemCount > 0) {
                val nextItem = (vp.currentItem + 1) % itemCount
                vp.currentItem = nextItem
                sliderHandler.postDelayed(this, 3000)
            }
        }
    }

    // ViewModel
    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(Repository()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityCommsViewModel.retryNetworkAction.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                viewModel.fetchNowPlayingMovies()
                viewModel.fetchTopRatedMovies()
                viewModel.fetchPopularMovies()

            }
        }

        binding.textSeeAllPopular.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionNavHomeToSeeAllFragmentPopular(
               listType = ListType.POPULAR_MOVIES_HOME , movieName = ""
            ))
        }

        binding.textSeeAllTopRated.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionNavHomeToSeeAllFragmentTopRated(
               listType = ListType.TOP_RATED_MOVIES , movieName = ""
            ))
        }

        // Restore LayoutManager state
        binding.recyclerViewMoviesByGenre.layoutManager?.onRestoreInstanceState(viewModel.layoutManagerState)

        // Movie Slider Adapter
        movieSliderAdapter = MovieSliderAdapter(emptyList()) { movieId , movieTitle->
            navigateToMovieDetails(movieId, "MovieSliderAdapter" ,movieTitle)
        }
        binding.movieSliderViewPager.adapter = movieSliderAdapter
        binding.movieSliderDotsIndicator.attachTo(binding.movieSliderViewPager)


        genreAdapter = GenreAdapter(emptyList()) { genre ->
            viewModel.fetchMoviesByGenre(genre.id)
        }
        binding.recyclerViewGenres.adapter = genreAdapter


        moviesByGenreAdapter = MovieAdapter(emptyList()) { movieId , movieTitle->
            navigateToMovieDetails(movieId, "MoviesByGenreAdapter",movieTitle)
        }
        binding.recyclerViewMoviesByGenre.adapter = moviesByGenreAdapter

        topRatedAdapter = MovieAdapter(emptyList()) { movieId , movieTitle->
            navigateToMovieDetails(movieId, "TopRatedAdapter",movieTitle)
        }
        binding.recyclerViewTopRated.adapter = topRatedAdapter

        popularMoviesAdapter = MovieAdapter(emptyList()) { movieId , movieTitle->
            navigateToMovieDetails(movieId, "PopularMoviesAdapter",movieTitle)
        }
        binding.recyclerViewPopularMovies.adapter = popularMoviesAdapter

        setupObservers()

        // Fetch initial data
        viewModel.fetchNowPlayingMovies() // For slider
        // Genres will be fetched, and then movies for the first genre
        viewModel.fetchTopRatedMovies()
        viewModel.fetchPopularMovies()
    }

    private fun navigateToMovieDetails(movieId: Int, adapterName: String, movieTitle: String) {
        Log.d("HomeFragment", "$adapterName: Clicked movie ID $movieId. Navigating.")
        try {
                val action = HomeFragmentDirections.actionNavHomeToMovieDetailFragment(movieId,movieTitle)
            findNavController().navigate(action)
        } catch (e: Exception) {

        }
    }

    private fun setupObservers() {
        viewModel.nowPlayingMovies.observe(viewLifecycleOwner) { movies ->
            movieSliderAdapter.updateMovies(movies.take(7))
            if (movies.isNotEmpty()) {
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        }

        viewModel.genres.observe(viewLifecycleOwner) { allGenres ->
            genreAdapter.updateData(allGenres)
            if (allGenres.isNotEmpty() && viewModel.moviesByGenre.value.isNullOrEmpty()) {
                viewModel.fetchMoviesByGenre(allGenres[0].id)
            }
        }

        viewModel.moviesByGenre.observe(viewLifecycleOwner) { movies ->
            moviesByGenreAdapter.updateMovies(movies)
        }

        viewModel.topRatedMovies.observe(viewLifecycleOwner) { movies ->
            topRatedAdapter.updateMovies(movies)
        }

        viewModel.popularMovies.observe(viewLifecycleOwner) { movies ->
            popularMoviesAdapter.updateMovies(movies)
        }
    }

    override fun onResume() {
        super.onResume()
        if (movieSliderAdapter.itemCount > 0) {
            sliderHandler.removeCallbacks(sliderRunnable) // Ensure no duplicates
            sliderHandler.postDelayed(sliderRunnable, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
        viewModel.layoutManagerState = binding.recyclerViewMoviesByGenre.layoutManager?.onSaveInstanceState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop any handlers
        sliderHandler.removeCallbacks(sliderRunnable)
        // Clear binding
        _binding = null
    }
}