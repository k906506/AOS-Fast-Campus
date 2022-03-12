package com.example.weather.data.models.airQuality


import com.google.gson.annotations.SerializedName

data class AirQualityResponse(
    @SerializedName("response")
    val response: Response?
)