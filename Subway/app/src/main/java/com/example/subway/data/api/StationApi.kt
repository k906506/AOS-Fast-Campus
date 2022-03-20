package com.example.subway.data.api

import com.example.subway.data.db.entity.StationEntity
import com.example.subway.data.db.entity.SubwayEntity

interface StationApi {
    suspend fun getStationDataUpdatedTimeMillis(): Long
    suspend fun getStationSubways(): List<Pair<StationEntity, SubwayEntity>>
}