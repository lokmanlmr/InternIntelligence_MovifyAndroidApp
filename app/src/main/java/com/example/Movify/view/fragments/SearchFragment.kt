package com.example.Movify.view.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.Movify.databinding.FragmentSearchBinding
import com.example.Movify.model.Repository
import com.example.Movify.view.adapters.MovieSearchAdapter
import com.example.Movify.viewmodel.SearchViewModel
import com.example.Movify.viewmodel.SearchViewModelFactory

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var movieSearchAdapter: MovieSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = Repository()
        val viewModelFactory = SearchViewModelFactory(repository)
        searchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

        setupRecyclerView()
        setupSearchInput()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        movieSearchAdapter = MovieSearchAdapter(emptyList()) { movie ->
            movie.id?.let { movieId ->
                try {
                    val action = SearchFragmentDirections.actionNavSearchToMovieDetailFragment(movieId,movie.title ?: "")
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error opening movie details.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(context, "Cannot open movie details: Movie ID missing.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchMoviesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = movieSearchAdapter
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()
                if (!query.isNullOrEmpty()) {
                    searchViewModel.searchMovies(query)
                } else {

                    movieSearchAdapter.updateMovies(emptyList())
                }
            }
        })
    }

    private fun observeViewModel() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { movies ->
            movieSearchAdapter.updateMovies(movies ?: emptyList())
            val isLoading = searchViewModel.isLoading.value ?: false
            binding.textNoResults.visibility = if (movies.isNullOrEmpty() && !isLoading && searchViewModel.errorMessage.value == null) View.VISIBLE else View.GONE
        }

        searchViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.textNoResults.visibility = View.GONE
                binding.textError.visibility = View.GONE
            }
        }

        searchViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.textError.text = errorMessage
                binding.textError.visibility = View.VISIBLE
                binding.textNoResults.visibility = View.GONE
            } else {
                binding.textError.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}