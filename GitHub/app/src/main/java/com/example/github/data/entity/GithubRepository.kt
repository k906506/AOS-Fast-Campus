package com.example.github.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GithubRepository")
data class GithubRepository(
    @PrimaryKey val name: String,
    val fullName: String?,
    @Embedded val owner: GithubOwner,
    val description: String?,
    val language: String?,
    val updateAt: String?,
    val stargazersCount: Int
)