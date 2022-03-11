package com.example.weather.data.models.monitoringStation


import com.google.gson.annotations.SerializedName

data class Body(
    @SerializedName("items")
    val monitoringStation: List<MonitoringStation>?,
    @SerializedName("numOfRows")
    val numOfRows: Int?,
    @SerializedName("pageNo")
    val pageNo: Int?,
    @SerializedName("totalCount")
    val totalCount: Int?
)