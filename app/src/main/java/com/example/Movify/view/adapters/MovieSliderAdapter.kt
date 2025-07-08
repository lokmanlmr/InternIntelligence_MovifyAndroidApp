package com.example.Movify.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Movify.R
import com.example.Movify.model.RetrofitClient
import com.example.Movify.model.dataclasses.Movie

class MovieSliderAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Int,String) -> Unit
) : RecyclerView.Adapter<MovieSliderAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewBackdrop: ImageView = itemView.findViewById(R.id.imageViewMovieBackdrop)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewMovieTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_slider, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        movie.backdropPath?.let { path ->
            val imageUrl = "${RetrofitClient.BACKDROP_BASE_URL}${path}"
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.baseline_downloading_24)
                .error(R.drawable.error)
                .into(holder.imageViewBackdrop)
        } ?: run {
            holder.imageViewBackdrop.setImageResource(R.drawable.error)
        }
        holder.textViewTitle.text = movie.title
        holder.itemView.setOnClickListener { movie.id?.let { onMovieClick(it,movie.title ?: "") } }
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        this.movies = newMovies
        notifyDataSetChanged()
    }
}