package ek.uw.kotlinemvprxjava.home

import android.content.Context

interface HomeContract{
    interface View{

        fun onDataFetched(weatherData: WeatherData?)
        fun onStoredDataFetched(weatherData: WeatherData?)
        fun onError()
        fun getContext(): Context

    }

    interface Presenter{

        fun subscribe(view: HomeContract.View)
        fun unSubscribe()
        fun refresh(lat: Double, long: Double)
    }
}