package com.example.Movify.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Movify.R
import com.example.Movify.model.ListType
import com.example.Movify.model.Repository
import com.example.Movify.view.adapters.MovieSearchAdapter
import com.example.Movify.viewmodel.SeeAllViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeeAllFragment : Fragment() {

    private val args: SeeAllFragmentArgs by navArgs()
    private lateinit var adapter: MovieSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_see_all, container, false)
        val listType = args.listType
        val movieId = args.movieId
        val movieTitle = args.movieName

        val recyclerView = view.findViewById<RecyclerView>(R.id.seeAllRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MovieSearchAdapter(emptyList()) { movie ->
            movie.id?.let { selectedMovieId ->
                val action = SeeAllFragmentDirections.actionSeeAllFragmentToMovieDetailFragment(
                    selectedMovieId,
                    movie.title ?: ""
                )
                findNavController().navigate(action)
            }
        }
        recyclerView.adapter = adapter

        when (listType) {
            ListType.POPULAR_MOVIES_HOME -> {
                viewModel.fetchPopularMovies()
                view.findViewById<TextView>(R.id.seeAllTitle).text = "Popular Movies"
            }

            ListType.TOP_RATED_MOVIES -> {
                viewModel.fetchTopRatedMovies()
                view.findViewById<TextView>(R.id.seeAllTitle).text = "Top Rated Movies"
            }

            ListType.SIMILAR_MOVIES -> {
                if (movieId != -1 && movieId != null) {
                    view.findViewById<TextView>(R.id.seeAllTitle).text =
                        "Similar Movies to $movieTitle"
                    viewModel.fetchRecommendedMovies(movieId)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Movie ID missing for similar movies",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


        viewModel.popularMovies.observe(viewLifecycleOwner) { movies ->
            if (listType == ListType.POPULAR_MOVIES_HOME) {
                adapter.updateMovies(movies)
            }
        }

        viewModel.topRatedMovies.observe(viewLifecycleOwner) { movies ->
            if (listType == ListType.TOP_RATED_MOVIES) {
                adapter.updateMovies(movies)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendedMovies.collectLatest { movies ->
                if (listType == ListType.SIMILAR_MOVIES) {
                    adapter.updateMovies(movies)
                }
            }
        }

        return view
    }

    private val viewModel: SeeAllViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SeeAllViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SeeAllViewModel(Repository()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }


}