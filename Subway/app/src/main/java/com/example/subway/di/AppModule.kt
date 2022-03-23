package com.example.subway.di

import android.app.Activity
import com.example.subway.data.api.StationApi
import com.example.subway.data.api.StationStorageApi
import com.example.subway.data.db.AppDatabase
import com.example.subway.data.preference.PreferenceManager
import com.example.subway.data.preference.SharedPreferenceManager
import com.example.subway.data.repository.StationRepository
import com.example.subway.data.repository.StationRepositoryImpl
import com.example.subway.presentation.stations.StationsContract
import com.example.subway.presentation.stations.StationsFragment
import com.example.subway.presentation.stations.StationsPresenter
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { Dispatchers.IO }

    // Database
    single { AppDatabase.build(androidApplication()) }
    single { get<AppDatabase>().stationDao() }

    // Preference
    single { androidContext().getSharedPreferences("preference", Activity.MODE_PRIVATE) }
    single<PreferenceManager> { SharedPreferenceManager(get()) }

    // Api
    single<StationApi> { StationStorageApi(Firebase.storage) }

    // Repository
    single<StationRepository> { StationRepositoryImpl(get(), get(), get(), get()) }

    // Presentation
    scope<StationsFragment> {
        scoped<StationsContract.Presenter> { StationsPresenter(getSource(), get()) }
    }
}