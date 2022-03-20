package com.example.subway.data.repository

import com.example.subway.domain.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    val stations: Flow<List<Station>>

    suspend fun refreshStations()
}