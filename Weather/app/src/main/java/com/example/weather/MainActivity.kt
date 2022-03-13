package com.example.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import com.example.weather.data.models.airQuality.Grade
import com.example.weather.data.models.airQuality.MeasuredValue
import com.example.weather.data.models.monitoringStation.MonitoringStation
import com.example.weather.data.util.RetrofitUtil
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var cancellationTokenSource: CancellationTokenSource? = null

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        bindViews()
        initVariables()
        requestLocationPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel()
        scope.cancel()
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }

    private fun requestBackgroundLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            backgroundPermissions,
            REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSIONS
        )
    }

    private fun bindViews() = with(binding) {
        refresh.setOnRefreshListener {
            fetchAirQualityData()
        }
    }

    private fun initVariables() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    companion object {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val backgroundPermissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
        const val REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSIONS = 101
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS && grantResults[0] == PackageManager.PERMISSION_GRANTED

        val backgroundLocationPermissionGranted =
            requestCode == REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSIONS && grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!backgroundLocationPermissionGranted) {
                requestBackgroundLocationPermissions()
            } else {
                fetchAirQualityData()
            }
        } else {
            if (!locationPermissionGranted) {
                finish()
            } else {
                fetchAirQualityData()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {
        cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource?.token
        ).addOnSuccessListener {
            scope.launch {
                binding.errorDescriptionTextView.isGone = true
                try {
                    val monitoringStation =
                        RetrofitUtil.getNearByMonitoringStation(it.latitude, it.longitude)
                    val measuredValue =
                        RetrofitUtil.getLatestAirQualityData(monitoringStation?.stationName!!)

                    displayAirQualityData(monitoringStation, measuredValue!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.errorDescriptionTextView.visibility = View.INVISIBLE
                    binding.contentLayout.alpha = 0F
                } finally {
                    binding.progressBar.isGone = true
                    binding.refresh.isRefreshing = false
                }
            }
        }
    }

    private fun displayAirQualityData(
        monitoringStation: MonitoringStation,
        measuredValue: MeasuredValue
    ) =
        with(binding) {
            contentLayout.animate()
                .alpha(1F)
                .start()

            measuringStationNameTextView.text = monitoringStation.stationName
            measuringStationAddressTextView.text = monitoringStation.addr

            (measuredValue.khaiGrade ?: Grade.UNKNOWN).let {
                root.setBackgroundResource(it.colorResId)
                totalGradeLabelTextView.text = it.label
                totalGradeEmojiTextView.text = it.emoji
            }

            with(measuredValue) {
                fineDustInformationTextView.text =
                    "미세먼지: ${pm10Value} ㎍/㎥ ${(pm10Grade ?: Grade.UNKNOWN).emoji}"
                ultraFineDustInformationTextView.text =
                    "초미세먼지: ${pm25Value} ㎍/㎥ ${(pm25Grade ?: Grade.UNKNOWN).emoji}"

                with(so2Item) {
                    labelTextView.text = "아황산가스"
                    gradeTextView.text = (so2Grade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$so2Value ppm"
                }

                with(coItem) {
                    labelTextView.text = "일산화탄소"
                    gradeTextView.text = (coGrade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$coValue ppm"
                }

                with(o3Item) {
                    labelTextView.text = "오존"
                    gradeTextView.text = (o3Grade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$o3Value ppm"
                }

                with(no2Item) {
                    labelTextView.text = "이산화질소"
                    gradeTextView.text = (no2Grade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$no2Value ppm"
                }
            }
        }
}