package com.example.Movify.model

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500" // For poster images
    const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780" // For slider backdrop images

    // Your TMDB API Key - REPLACE WITH YOUR ACTUAL KEY
    const val API_KEY = "" // <<< IMPORTANT: YOU MUST SPECIFY YOUR OWN API KEY HERE

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val tmdbApiService: TmdbApiService by lazy {
        retrofit.create(TmdbApiService::class.java)
    }
}