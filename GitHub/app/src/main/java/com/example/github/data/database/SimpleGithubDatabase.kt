package com.example.github.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.github.data.dao.GithubDao
import com.example.github.data.entity.GithubRepository

@Database(entities = [GithubRepository::class], version = 1)
abstract class SimpleGithubDatabase : RoomDatabase() {
    abstract fun githubDao(): GithubDao

    companion object {
        private var instance: SimpleGithubDatabase? = null

        @Synchronized
        fun getInstance(context: Context): SimpleGithubDatabase? {
            if (instance == null) {
                synchronized(SimpleGithubDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        SimpleGithubDatabase::class.java,
                        "github-database"
                    ).build()
                }
            }
            return instance
        }
    }
}