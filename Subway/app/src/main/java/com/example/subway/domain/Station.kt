package com.example.subway.domain

data class Station(
    val name : String,
    val isFavorited : Boolean,
    val connectedSubways : List<Subway>
)
