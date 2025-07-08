package com.example.Movify.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Movify.R
import com.example.Movify.model.Repository
import com.example.Movify.view.adapters.MovieSearchAdapter
import com.example.Movify.viewmodel.SavedViewModel
import com.example.Movify.viewmodel.SavedViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class SavedFragment : Fragment() {

    private lateinit var savedViewModel: SavedViewModel
    private lateinit var movieAdapter: MovieSearchAdapter
    private lateinit var recyclerView: RecyclerView
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val movieRepository = Repository()
        val firebaseAuth = FirebaseAuth.getInstance()
        val factory = SavedViewModelFactory(movieRepository, firebaseAuth)
        savedViewModel = ViewModelProvider(this, factory).get(SavedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_saved, container, false)
        recyclerView = view.findViewById(R.id.search_movies_recycler_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        savedViewModel.loadSavedMovies()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieSearchAdapter(emptyList()) { movie ->
            val action = movie.id?.let { movieId ->
                SavedFragmentDirections.actionNavSavedToMovieDetailFragment(movieId , movie.title ?: "")
            }
            if (action != null) {
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, "Cannot open movie details: ID missing.", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.apply {
            adapter = movieAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        savedViewModel.savedMoviesList.observe(viewLifecycleOwner) { movies ->
            val noResultsTextView: android.widget.TextView? = view?.findViewById(R.id.text_no_results)
            if (movies.isNullOrEmpty() && savedViewModel.error.value == null && !savedViewModel.isLoading.value!! ) {
                noResultsTextView?.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noResultsTextView?.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            movieAdapter.updateMovies(movies ?: emptyList())
            Log.d("SavedFragment", "Adapter updated with ${movies?.size ?: 0} movies.")
        }

        savedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d("SavedFragment", "isLoading state: $isLoading")
            if (isLoading) {
                view?.findViewById<android.widget.TextView>(R.id.text_no_results)?.visibility = View.GONE
                view?.findViewById<android.widget.TextView>(R.id.text_error)?.visibility = View.GONE
            }
        }

        savedViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            val errorTextView: android.widget.TextView? = view?.findViewById(R.id.text_error)
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                errorTextView?.text = it
                errorTextView?.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Log.e("SavedFragment", "Error observed: $it")
            } ?: run {
                errorTextView?.visibility = View.GONE
            }
        }
    }
}