package com.example.subway.presenter

interface BaseView<PresenterT : BasePresenter> {
    val presenter: PresenterT
}