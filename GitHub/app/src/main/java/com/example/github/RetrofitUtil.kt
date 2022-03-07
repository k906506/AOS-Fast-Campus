package com.example.github

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {
    val authApiService: AuthApiService by lazy { getGithubRetrofit().create(AuthApiService::class.java) }

    private fun getGithubRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Key.GITHUB_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}