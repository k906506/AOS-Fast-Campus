package com.example.subway.presentation.stations

import com.example.subway.domain.Station
import com.example.subway.presenter.BasePresenter
import com.example.subway.presenter.BaseView

interface StationsContract {
    interface View : BaseView<Presenter> {
        fun showLoadingIndicator()

        fun hideLoadingIndicator()

        fun showStations(stations: List<Station>)
    }

    interface Presenter : BasePresenter {
        fun filterStations(query: String)
    }
}