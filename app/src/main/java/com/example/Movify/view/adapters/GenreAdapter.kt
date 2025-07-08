package com.example.Movify.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Movify.R
import com.example.Movify.model.dataclasses.Genre


class GenreAdapter(
    private var genres: List<Genre>,
    private val onGenreClick: (Genre) -> Unit
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    private var selectedPosition = 0

    inner class GenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textGenreName: TextView = itemView.findViewById(R.id.textGenreName)

        fun bind(genre: Genre, isSelected: Boolean) {
            textGenreName.text = genre.name
            textGenreName.setBackgroundResource(
                if (isSelected) R.drawable.bg_genre_oval else R.drawable.bg_genre_oval2
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genre, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position]
        holder.bind(genre, position == selectedPosition)
        holder.itemView.setOnClickListener {
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
            onGenreClick(genre)
        }
    }

    override fun getItemCount(): Int = genres.size

    fun updateData(newGenres: List<Genre>) {
        genres = newGenres
        notifyDataSetChanged()
    }
}