package ek.uw.kotlinemvprxjava.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import ek.uw.kotlinemvprxjava.R
import kotlinx.android.synthetic.*
import org.w3c.dom.Text

class HomeActivity: AppCompatActivity(), HomeContract.View{

    private val RC_ENABLE_LOCATION = 1
    private val RC_LOCATION_PERMISSION = 2
    private val TAG_FORECAST_DIALOG = "forecast_dialog"
    var mPresenter: HomeContract.Presenter? = null
    var mLocationManager: LocationManager? = null
    var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    var mLocation: Location? = null

    var mLocationListener: LocationListener = object : LocationListener{
        override fun onLocationChanged(location: Location?) {
            mSwipeRefreshLayout?.isRefreshing = true
            mPresenter?.refresh(location?.latitude ?: 0.0, location?.longitude ?: 0.0)

            if (location?.latitude != null && location.latitude != 0.0 && location.longitude != 0.0){
                mLocation = location
                mLocationManager?.removeUpdates(this)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onProviderDisabled(provider: String?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onProviderEnabled(provider: String?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_home)

            mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout) as SwipeRefreshLayout
            mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            mPresenter = HomePresenter()
            mPresenter?.subscribe(this)

            initViews()

            if (checkAndAskForLocationPermissions()){
                checkGPSEnabledAndPrompt()
            }

        }
        private fun initViews(){
            mSwipeRefreshLayout?.setOnRefreshListener {
                if(mLocation != null){
                    mPresenter?.refresh(mLocation?.latitude ?: 0.0, mLocation?.longitude ?: 0.0)
                }else{
                    mSwipeRefreshLayout?.isRefreshing = false
                }
            }
        }

        override fun getContext() = this

        override fun onStoredDataFetched(weatherData: WeatherData?){
            updateUI(weatherData)
        }
        override fun onDataFetched(weatherData: WeatherData?){
            mSwipeRefreshLayout?.isRefreshing = false
            updateUI(weatherData)
        }

        private fun updateUI(weatherData: WeatherData?){
            val tempTextView = findViewById(R.id.temperature_text_view) as TextView
            val windSpeedTextView = findViewById<TextView>(R.id.wind_speed_text_view)
            val humidityTextView = findViewById<TextView>(R.id.humidity_text_view)
            val weatherImageView = findViewById<ImageView>(R.id.weather_image_view)
            val weatherConditionTextView = findViewById<TextView>(R.id.weather_condition_text_view)
            val cityNameTextView = findViewById<TextView>(R.id.city_name_text_view)

            val formattedTempText = String.format(getString(R.string.celcius_temp), weatherData?.query?.results?.channel?.item?.condition?.temp ?: "")

            tempTextView.text = formattedTempText
            windSpeedTextView.text = "${weatherData?.query?.results?.channel?.wind?.speed ?: ""} km/h"
            humidityTextView.text = "${weatherData?.query?.results?.channel?.atmosphere?.humidity ?: ""} %"
            val weatherCode = weatherData?.query?.results?.channel?.item?.condition?.code ?: "3200"
            weatherImageView.setImageResource(WeatherToImage.getImageForCode(weatherCode))
            weatherConditionTextView.text = weatherData?.query?.results?.channel?.item?.condition?.text ?: ""

            val city = weatherData?.query?.results?.channel?.location?.city ?: ""
            val country = weatherData?.query?.results?.channel?.location?.country ?: ""
            val region = weatherData?.query?.results?.channel?.location?.region ?: ""
            cityNameTextView.text = "${city.trim()}, ${region.trim()}, ${country.trim()}"
            val forecastRecyclerView = findViewById(R.id.forecast_recycler_view) as RecyclerView
            val forecastRecyclerAdapter = ForecastRecyclerAdapter(this, weatherData?.query?.results?.channel?.item?.forecast?.asList())
            forecastRecyclerAdapter.addActionListener {
                    forecast ->
                val forecastDialog = ForecastDialogFragment.getInstance(forecast)
                forecastDialog.show(supportFragmentManager, TAG_FORECAST_DIALOG)

            }
            forecastRecyclerView.adapter = forecastRecyclerAdapter
            forecastRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        }

        override fun onError(){
            mSwipeRefreshLayout?.isRefreshing = false

            val coordinatorLayout = findViewById<CoordinatorLayout>(R.id.coordinator_layout)

            val retrySnackBar = Snackbar.make(coordinatorLayout, "Unable To Fetch Data", Snackbar.LENGTH_INDEFINITE)
            retrySnackBar.setAction("Retry"){
                v->
                mPresenter?.refresh(mLocation?.latitude ?: 0.0, mLocation?.longitude ?: 0.0)
                mSwipeRefreshLayout?.isRefreshing = true
                retrySnackBar.dismiss()
            }
            retrySnackBar.setActionTextColor(ContextCompat.getColor(this, R.color.white))
            retrySnackBar.show()
        }
        override fun onDestroy(){
            super.onDestroy()
            mPresenter?.onSubscribe()
            mLocationManager?.removeUpdates(mLocationListener)
        }

        private fun checkGPSEnabledAndPrompt(){
            val isLocationEnabled = mLocationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isLocationEnabled){
                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("GPS Is Not Enabled")
                    .setMessage("This app requires GPS to get the forecast")
                    .setPositiveButton(android.R.string.ok){
                        dialog, which ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivityForResult(intent, RC_ENABLE_LOCATION)

                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel){
                        dialog, which ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }else(
                    requestLocationUpdates()
                    )
        }

    }

    private fun requestLocationUpdates(){
        val provider = LocationManager.NETWORK_PROVIDER

        mLocationManager?.requestLocationUpdates(provider, 0, 0.0f, mLocationListener)

        val location = mLocationManager?.getLastKnownLocation(provider)
        mLocationListener.onLocationChanged(location)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            RC_ENABLE_LOCATION ->{
                checkGPSEnabledAndPrompt()
            }
        }
    }
    private fun checkAndAskForLocationPermissions(): Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), RC_LOCATION_PERMISSION)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RC_LOCATION_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPSEnabledAndPrompt()
                } else {
                    checkAndAskForLocationPermissions()
                }
            }
        }
    }
}
