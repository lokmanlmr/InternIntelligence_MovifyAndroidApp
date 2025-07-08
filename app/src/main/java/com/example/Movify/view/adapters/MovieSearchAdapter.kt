package com.example.Movify.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.Movify.R
import com.example.Movify.databinding.ItemMovieSearchBinding
import com.example.Movify.model.dataclasses.Movie
import com.bumptech.glide.Glide
import com.example.Movify.model.RetrofitClient

class MovieSearchAdapter(
    private var movies: List<Movie>,
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieSearchAdapter.MovieViewHolder>() {

    inner class MovieViewHolder(private val binding: ItemMovieSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.apply {
                textTitle.text = movie.title
                textDate.text = movie.releaseDate
                textRating.text = String.format("%.1f", movie.voteAverage)
                textDescription.text = movie.overview

                // Load image using Glide
                movie.posterPath?.let {
                    val imageUrl = RetrofitClient.IMAGE_BASE_URL + it
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(imageBackground)
                } ?: run {
                    Glide.with(itemView.context).clear(imageBackground)
                    imageBackground.setImageResource(R.drawable.ic_launcher_background)
                }

                root.setOnClickListener { onItemClick(movie) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}