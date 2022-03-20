package com.example.subway.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao() : StationDao

    companion object {
        private const val DATABASE_NAME = "station.db"

        fun build(context: Context):AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
    }
}