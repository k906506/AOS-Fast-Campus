package com.example.weather.data.service

import com.example.weather.data.apiKey.ApiKey
import com.example.weather.data.models.airQuality.AirQualityResponse
import com.example.weather.data.models.monitoringStation.MonitoringStationsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AirKoreaApiService {
    @GET(
        "B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList" +
                "?serviceKey=${ApiKey.AIRKOREA_API_KEY}" +
                "&returnType=json"
    )
    suspend fun getNearByMonitoringStation(
        @Query("tmX") tmX: Double,
        @Query("tmY") tmY: Double,
    ): Response<MonitoringStationsResponse>

    @GET(
        "B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?serviceKey=${ApiKey.AIRKOREA_API_KEY}" +
                "&returnType=json" +
                "&dataTerm=DAILY" +
                "&ver=1.3"
    )
    suspend fun getRealtimeAirQualities(
        @Query("stationName") stationName: String
    ) : Response<AirQualityResponse>
}