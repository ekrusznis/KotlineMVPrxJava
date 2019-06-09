package ek.uw.kotlinemvprxjava.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.*

interface MetaWeatherApi{
    @GET("weatherdata")
    fun getLocationDetails(@Header("X-Mashape-Key") key: String, @Header("Accept") type: String,
                           @Query("lat") lat: Double, @Query("lng") lng: Double): Observable<WeatherData>
}