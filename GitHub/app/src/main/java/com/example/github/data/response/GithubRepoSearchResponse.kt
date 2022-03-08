package com.example.github.data.response

import com.example.github.data.entity.GithubRepository

data class GithubRepoSearchResponse(
    val totalCount: Int,
    val items: List<GithubRepository>
)