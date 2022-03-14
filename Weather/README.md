<p align="center">
<img src="https://images.velog.io/images/k906506/post/59dc32a9-7f1a-46ea-a6b5-9587f067243e/ezgif.com-gif-maker%20(6).gif" height="200px" width="395px">
  </p>

# 키워드

1. 카카오톡, 공공데이터 API
2. Retrofit, OkHttpLogger
3. FusedLocationProvider
4. Coroutine
5. AppWidgetProvider
6. Service Lifecycle

## 위치 정보 가져오기

우선 사용자의 위치 정보를 불러와야 한다. 위치 정보를 가져오기 위해서 `권한` 을 설정한다.

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

권한을 설정했으면 `requestPermission` 과 `onRequestPermissionResult` 를 구현한다. `requestPermission` 은 권한을 요청하고 `onRequestPermissonResult` 는 권한 요청의 결과를 확인한다.

### 요청할 권한 정의

우선 `companion object` 로 요청할 권한들을 정의했다.

```kotlin
	companion object {
        private val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private val backgroundPermissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
        const val REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSIONS = 101
    }
```

### 사용자에게 권한 요청

요청할 권한들과 권한에 대한 고유값을 입력한다.

```kotlin
	private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }
```

### 요청한 권한 확인

`Background Location` 을 구현하니 코드가 좀 지저분해졌다. 우선 `requestCode` 와 `grantResults` 를 확인한다. `grantResults` 에는 권한 부여의 결과가 들어있다. 사실 이렇게 구현하면 어플을 처음 실행했을 때 권한을 부여받고 이 때 사용자가 거부한 경우 어플이 실행되자마자 종료되는 오류에 빠지게 된다. `onCheckPermission` 을 활용하여 좀 더 구체적으로 구현해야하지만 일단은 이렇게 구현했다. 권한이 성공적으로 부여된 경우 `fetchAirQualityData` 를 호출한다.

```kotlin
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
```

 때 사용할 수 있는 것은 `LocationManager` 와 `FusedLocation`  이 있다. 

## 근접 측정소 정보 불러오기

`fusedLocationProvider` 를 활용하여 현재 사용자의 위치에 대한 위도와 경도를 받아온다. 위 데이터를 가지고 근처에 있는 대기 측정소의 정보를 불러온다. 이 때 `한국환경공단` 에서 제공하는 `API` 를 사용했다. 해당 API를 사용하기 이전에 좌표 변환을 시켜줘야 한다. 대기 측정소의 정보를 불러오는 API 에서는 좌표 값으로 위도, 경도가 아닌 `TM` 을 사용한다. 따라서 좌표 변환을 우선적으로 진행해야 한다. 이때는 `카카오 좌표 변환 API` 를 사용했다. 

### 위도, 경도 좌표계를 TM 좌표계로 변환

`카카오 API` 를 사용하면 좌표변환을 손쉽게 할 수 있다. `Api response` 에 대한 `Model` 과 `Interface` 를 구현한다. 

```kotlin
package com.example.weather.data.service

import com.example.weather.data.apiKey.ApiKey
import com.example.weather.data.models.tmcoordinates.TmCoordinatesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface KakaoLocalApiService {
    @Headers("Authorization: KakaoAK ${ApiKey.KAKAO_API_KEY}")
    @GET("v2/local/geo/transcoord.json?output_coord=TM")
    suspend fun getTmCoordinates(
        @Query("x") longitude: Double,
        @Query("y") latitude: Double
    ): Response<TmCoordinatesResponse>
}
```

이후 `object` 로 `Singleton` 으로 구현한 `Retrofit` 과 연결해준다. 

```kotlin
object RetrofitUtil {
    suspend fun getNearByMonitoringStation(
        latitude: Double,
        longitude: Double
    ): MonitoringStation? {
        val tmCoordinates = kakaoLocalApiService.getTmCoordinates(longitude, latitude)
            .body()
            ?.documents
            ?.firstOrNull()

        val tmX = tmCoordinates?.x
        val tmY = tmCoordinates?.y
}

private val kakaoLocalApiService: KakaoLocalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Url.KAKAO_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildHttpClient())
            .build()
            .create()
    }

private fun buildHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // 디버그할 때만 로깅이 찍히게
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            ).build()
    }
```

좌표 변환이 완료되었으니 이제 `공공 API` 를 사용해서 근접 측정소의 정보를 불러온다.

### 근접 측정소 정보 불러오기

```kotlin
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
}
```

```kotlin
		private val airKoreaApiService: AirKoreaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Url.AIRKOREA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildHttpClient())
            .build()
            .create()
    }
```

이렇게 `Interface` 를 구현하고 변환한 TM 좌표를 넣어준다. 아래처럼 근접 측정소의 정보가 출력되는 것을 볼 수 있다.

```json
{
   "response":{
      "body":{
         "totalCount":3,
         "items":[
            {
               "tm":4.1,
               "addr":"경기도 여주시 가남읍 태평중앙1길 20가남읍행정복지센터 옥상",
               "stationName":"가남읍"
            },
            {
               "tm":8.2,
               "addr":"경기도 이천시 부발읍 무촌로 117부발보건지소 옥상",
               "stationName":"부발읍"
            },
            {
               "tm":9.3,
               "addr":"경기 이천시 설성면 신필리산 88-5(전파연구소 입구)",
               "stationName":"설성면"
            }
         ],
         "pageNo":1,
         "numOfRows":10
      },
      "header":{
         "resultMsg":"NORMAL_CODE",
         "resultCode":"00"
      }
   }
}
```

## 실시간 대기 정보 불러오기

근접 측정소의 실시간 대기 정보를 불러오기 위해선 또 다른 API 를 사용해야 한다. 이를 위해 `Interface` 를 추가로 정의해줬다.

```kotlin
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
```

이후 전에 정의했던 `fetchAirQualityData` 에서 활용한다.

```kotlin
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
                }
            }
        }
    }
```

<p align="center">
<img src="https://images.velog.io/images/k906506/post/56347a18-02fa-49dc-b391-73a94ae9b2ee/ezgif.com-gif-maker%20(5).gif" height="200px" width="395px">
  </p>
  
## 디자인과 기능 추가

뭔가 좀 아쉽다. 데이터를 불러오기까지 흰색 화면이 뜨는 것과 그냥 팍! 하고 뜨는 것, 그리고 위에서 아래로 `Swipe` 를 하면 정보를 새로 불러올 수 있게 수정하려고 한다. 

### ProgressBar 추가

우선 앱이 실행될 때 `ProgressBar` 가 먼저 보여지고 데이터를 성공적으로 불러온 경우에 `isGone` 을 활용해서 `ProgressBar` 를 제거해주는 쪽으로 구현했다.

### 자연스러운 화면 전환

`animate` 를 사용해서 `alpha` 를 0에서 1로 전환하는 쪽으로 구현했다. 

### Swipe 기능 추가

`Swipe` 를 사용하기 위해선 해당 레이아웃을 `SwipeRefreshLayout` 으로 구현하면 된다. 그 전에 라이브러리에 대한 의존성을 추가한다. 

```xml
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
```

이후 콜백 함수인 `setOnRefreshListener` 를 통해서 정보를 새로 갱신할 수 있게 한다.

```kotlin
	private fun bindViews() = with(binding) {
        refresh.setOnRefreshListener {
            fetchAirQualityData()
        }
    }
```

## 위젯

안드로이드 위젯을 구현하기 위해선 `AppWidgetProvider` 를 상속받는 클래스를 구현해야한다. 

### Receiver 등록

사용자가 위젯을 업데이트하려고 할 때 이를 수신하기 위해 `Recevier` 를 등록한다. 

```xml
		<receiver
            android:name=".widget.SimpleAirQualityWidgetProvider"
            android:exported="false">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_simple_info" />
        </receiver>
```

`meta-data` 에는 `위젯` 에 대한 정보가 담겨 있다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/widget_simple"
    android:minWidth="110dp"
    android:minHeight="50dp"
    android:resizeMode="none"
    android:updatePeriodMillis="3600000"
    android:widgetCategory="home_screen" />
```

### 채널 설정

`Android SDK` `26` 부터는 포그라운드 서비스가 실행될 때 이를 알림으로 표시해야 한다. 이를 위해 먼저 `채널` 을 설정해줬다.

```kotlin
		private funcreateNotification(): Notification =
			  NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_baseline_refresh_24)
        .setChannelId(WIDGET_REFRESH_CHANNEL_ID)
        .build()
```

```kotlin
			private fun createChannelIfNeeded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                    ?.createNotificationChannel(
                        NotificationChannel(
                            WIDGET_REFRESH_CHANNEL_ID,
                            "위젯 갱신 채널",
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )
            }
        }
```
