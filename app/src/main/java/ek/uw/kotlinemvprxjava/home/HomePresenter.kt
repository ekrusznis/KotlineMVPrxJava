package ek.uw.kotlinemvprxjava.home

import android.content.Context
import com.google.gson.Gson
import ek.uw.kotlinemvprxjava.Constants
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.lang.Exception

class HomePresenter: HomeContract.Presenter{
    var mView: HomeContract.View? = null

    override fun subscribe(view: HomeContract.View){
        mView = view

        val storedWeather = getFileFromStorage(mView?.getContext())
        if (storedWeather != null){
            mView?.onStoredDataFetched(storedWeather)
        }
    }

    override fun onSubscribe(){
        mView = null
    }

    override fun refresh(lat: Double, long: Double){
        NetworkService.getMetaWeatherApi()
            .getLocationDetails(Secret.API+_KEY, Constants.TYPE_TEXT_PLAIN, lat, long)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                weatherData ->
                mView?.onDataFetched(weatherData)
                storeFileToExternalStorage(weatherData, mView?.getContext())
            }, {error ->
                mView?.onError()
            })
    }

    private fun storeFileToExternalStorage(weatherData: WeatherData, context: Context){
        val gson = Gson()
        val weatherJson = gson.toJson(weatherData)

        val weatherFile = File(mView?.getContext()?.filesDir, Constants.WEATHER_FILE_NAME)
        if (weatherFile.exists()) weatherFile.delete()
        weatherFile.createNewFile()

        val outputStream = mView?.getContext()?.openFileOutput(Constants.WEATHER_FILE_NAME, Context.MODE_PRIVATE)
        outputStream?.write(weatherJson.toByteArray())
        outputStream?.close()
    }

    private fun getFileFromStorage(context: Context?): WeatherData?{
        try {
            val weatherFile = File(context?.filesDir, Constants.WEATHER_FILE_NAME)
            val weatherJson = weatherFile.readText()
            val gson = Gson()
            val weatherData = gson.fromJson(weatherJson, WeatherData::class.java)
            return weatherData
        }catch (e: Exception){
            return null
        }
    }
}