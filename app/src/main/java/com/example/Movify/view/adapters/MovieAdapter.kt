package com.example.Movify.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Movify.R
import com.example.Movify.model.dataclasses.Movie

class MovieAdapter(
    private var movies: List<Movie?>,
    private val onMovieClick: (Int,String) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageBackground: ImageView = itemView.findViewById(R.id.imageBackground)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)

        fun bind(movie: Movie?, onMovieClick: (Int,String) -> Unit) {
            if (movie == null) {
                textTitle.text = "Unknown Title"
                Glide.with(itemView.context)
                    .load(R.drawable.error)
                    .into(imageBackground)
            } else {
                textTitle.text = movie.title ?: "Unknown Title"
                val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath ?: ""}"
                Glide.with(itemView.context)
                    .load(posterUrl)
                    .placeholder(R.drawable.error)
                    .into(imageBackground)
                itemView.setOnClickListener { onMovieClick(movie.id ?: -1 ,movie.title ?:"") }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position], onMovieClick)
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}