package com.example.github

import com.example.github.utility.AuthApiService
import com.example.github.utility.GithubApiService
import com.example.github.utility.Key
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {
    val authApiService: AuthApiService by lazy {
        getGithubAuthRetrofit().create(AuthApiService::class.java)
    }

    private fun getGithubAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Key.GITHUB_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val githubApiSearchService: GithubApiService by lazy {
        getGithubSearchRetrofit().create(
            GithubApiService::class.java
        )
    }

    private fun getGithubSearchRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Key.GITHUB_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}