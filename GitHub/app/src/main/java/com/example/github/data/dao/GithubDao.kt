package com.example.github.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.github.data.entity.GithubRepository

@Dao
interface GithubDao {
    @Insert
    suspend fun insert(repo: GithubRepository)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repoList: List<GithubRepository>)

    @Query("SELECT * FROM githubrepository")
    suspend fun getHistory(): List<GithubRepository>

    @Query("DELETE FROM githubrepository")
    suspend fun clearAll()
}