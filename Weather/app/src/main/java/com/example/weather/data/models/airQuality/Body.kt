package com.example.weather.data.models.airQuality


import com.google.gson.annotations.SerializedName

data class Body(
    @SerializedName("items")
    val measuredValue: List<MeasuredValue>?,
    @SerializedName("numOfRows")
    val numOfRows: Int?,
    @SerializedName("pageNo")
    val pageNo: Int?,
    @SerializedName("totalCount")
    val totalCount: Int?
)