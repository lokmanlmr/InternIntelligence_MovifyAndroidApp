package com.example.Movify.model.dataclasses

data class Review(
    // Firestore's document ID will be placed here when reading.
    // When writing, this field is not sent to Firestore directly if you use a HashMap.
    val id :String = "",
    val author :String = "",
    val content :String = "",
    val rating :Double = 0.0,
    val date :String = "",
    val movieId: Int = 0
)