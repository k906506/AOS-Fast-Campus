package com.example.subway.data.db.entity.mapper

import com.example.subway.data.db.entity.StationWithSubwaysEntity
import com.example.subway.data.db.entity.SubwayEntity
import com.example.subway.domain.Station
import com.example.subway.domain.Subway

fun StationWithSubwaysEntity.toStation() = Station(
    name = station.stationName,
    isFavorited = station.isFavorited,
    connectedSubways = subways.toSubways()
)

fun List<StationWithSubwaysEntity>.toStations() = map { it.toStation() }

fun List<SubwayEntity>.toSubways(): List<Subway> = map { Subway.findById(it.subwayId) }