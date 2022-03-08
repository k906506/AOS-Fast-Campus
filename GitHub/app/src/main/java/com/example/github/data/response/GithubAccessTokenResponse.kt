package com.example.github.data.response

data class GithubAccessTokenResponse(
    val accessToken: String,
    val scope: String,
    val tokenType: String
)
