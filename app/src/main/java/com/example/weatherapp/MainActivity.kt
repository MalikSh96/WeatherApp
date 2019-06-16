package com.example.weatherapp

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.*
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {


    private lateinit var weatherFont: Typeface
    private var city : String = "Copenhagen"
    private val OPEN_WEATHER_MAP_API : String = "34910ce547d9588c2b86389c14d95e5c"
    private lateinit var permissions : Array<String>
    private lateinit var currentLocation: Location
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        weatherFont = Typeface.createFromAsset(assets, "fonts/weathericons-regular-webfont.ttf")
        weather_icon.typeface = weatherFont

        selectCity.setOnClickListener {
            val alertDialog : AlertDialog.Builder  = AlertDialog.Builder(this@MainActivity)
            alertDialog.setTitle("Change City")
            val input = EditText(this@MainActivity) //gives
            input.setText(city)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.layoutParams = lp
            alertDialog.setView(input)
            alertDialog.setPositiveButton("Change"){ _ : DialogInterface, _ : Int ->
                city = input.text.toString()
                loadCityWeather(city)
            }
            alertDialog.setNegativeButton("Cancel") { dialog : DialogInterface, _ : Int -> dialog.cancel() }
            alertDialog.show()
        }

        permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions,0) //Helper for accessing features in Activity.
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == -1) {
            loadCityWeather(city)
        } else {
            updateLocation()
        }
    }

    /**
     * @param location the location which coordinates will be used to gather weather information
     *
     * Initiates the process of loading the current weather of a given location
     */
    private fun loadLocationWeather(location: Location) {
        if (HTTP.isNetworkAvailable(applicationContext)) {
            val task = getWeatherLocation()
            task.execute(location.latitude.toString(), location.longitude.toString())
        } else {
            toast("No Internet Connection")
        }
    }

    private fun updateLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return
        }
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isCostAllowed = false
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
                return
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this)
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            currentLocation = location
            loadLocationWeather(currentLocation)
            locationManager.removeUpdates(this)
        }
    }
    override fun onProviderEnabled(provider: String?) { TODO("not implemented") }
    override fun onProviderDisabled(provider: String?) { TODO("not implemented") }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { TODO("not implemented") }

    /**
     * Inner class, extends abstract class AsyncTask that does an operation on background thread, and updates UI/Main thread
     *
     * @constructor Creates an instance with an execute() method that takes a String completing an async operation
     */
    inner class getWeatherLocation : AsyncTask<String, Void, String>() { //<background, type to define progress, return from background - postExecute>
        //before any loads - initial setup
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
        }

        /**
         * @param args latitude and longitude - vararg parameter
         * Handles HTTP message on background thread and initiates [onPostExecute] on completion
         * @return String? response in JSON format
         */
        override fun doInBackground(vararg args : String): String? { //varag as many of String

            return HTTP.executeGet("http://api.openweathermap.org/data/2.5/weather?lat=${args[0]}&lon=${args[1]}"
                    + "&appid=${OPEN_WEATHER_MAP_API}&units=metric")
        }

        /**
         * @param json a HTTP response in a String JSON format
         * Handles the generation of the UI according to the data from the HTTP response
         * @return Void
         */
        override fun onPostExecute(json: String) {
            try {
                val json = JSONObject(json)
                if (json != null) { //equates to null if string is empty
                    val details = json.getJSONArray("weather").getJSONObject(0)
                    val main = json.getJSONObject("main")
                    val df = DateFormat.getDateTimeInstance()
                    city_field.text     =   getString(R.string.city_field, json.getString("name").toUpperCase(Locale.US),
                        json.getJSONObject("sys").getString("country"))
                    details_field.text  =   details.getString("description").toUpperCase(Locale.US)
                                            current_temperature_field.text = getString(R.string.current_temperature_field,
                                            main.getString("temp"))
                    humidity_field.text =   getString(R.string.humidity_field, main.getString("humidity"))
                    pressure_field.text =   getString(R.string.pressure_field, main.getString("pressure"))
                    updated_field.text  =   df.format(Date(json.getLong("dt") * 1000))
                    weather_icon.text   =   Html.fromHtml(
                        setWeatherIcon(
                            details.getInt("id"),
                            json.getJSONObject("sys").getLong("sunrise") * 1000,
                            json.getJSONObject("sys").getLong("sunset") * 1000
                        )
                    )

                    loader.visibility = View.GONE
                }
            } catch (e: JSONException) {
            Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_LONG).show()
                Toast.makeText(applicationContext, "Error! Perhaps you've misspelled the city?", Toast.LENGTH_SHORT).show()
                loader.visibility = View.GONE
            }
        }
    }

    /**
     * @param city the city to load
     *
     * Initiates the process of loading the current weather of a given city
     */
    private fun loadCityWeather(city: String) {
        if (HTTP.isNetworkAvailable(applicationContext)) {
            val task = getWeatherCity()
            task.execute(city)
        } else {
            Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Inner class, extends abstract class AsyncTask that does an operation on background thread, and updates UI/Main thread
     *
     * @constructor Creates an instance with an execute() method that takes a String completing an async operation
     */
    inner class getWeatherCity : AsyncTask<String, Void, String>() { //<backgroudn argument, type to define progress, return from background to postExecute>
        //before any loads - initial setup
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
        }

        /**
         * @param args city name - vararg parameter
         * Handles HTTP message on background thread and initiates [onPostExecute] on completion
         * @return String? response in JSON format
         */
        override fun doInBackground(vararg args : String): String? {
            return HTTP.executeGet("http://api.openweathermap.org/data/2.5/weather?q=" + args[0] +
                    "&units=metric&appid=" + OPEN_WEATHER_MAP_API
            )
        }

        /**
         * @param json a HTTP response in a String JSON format
         * Handles the generation of the UI according to the data from the HTTP response
         * @return Void
         */
        override fun onPostExecute(json: String) {
            try {
                val json = JSONObject(json)
                if (json != null) { //equates to null if string is empty
                    val details = json.getJSONArray("weather").getJSONObject(0)
                    val main = json.getJSONObject("main")
                    val df = DateFormat.getDateTimeInstance()
                    city_field.text     =   getString(R.string.city_field, json.getString("name").toUpperCase(Locale.US),
                        json.getJSONObject("sys").getString("country"))
                    details_field.text  =   details.getString("description").toUpperCase(Locale.US)
                    current_temperature_field.text = getString(R.string.current_temperature_field,
                        main.getString("temp"))
                    humidity_field.text =   getString(R.string.humidity_field, main.getString("humidity"))
                    pressure_field.text =   getString(R.string.pressure_field, main.getString("pressure"))
                    updated_field.text  =   df.format(Date(json.getLong("dt") * 1000))
                    weather_icon.text   =   Html.fromHtml(
                        setWeatherIcon(
                            details.getInt("id"),
                            json.getJSONObject("sys").getLong("sunrise") * 1000,
                            json.getJSONObject("sys").getLong("sunset") * 1000
                        )
                    )

                    loader.visibility = View.GONE
                }
            } catch (e: JSONException) {
                Toast.makeText(applicationContext, "Error! Perhaps you've misspelled the city?", Toast.LENGTH_SHORT).show()
                loader.visibility = View.GONE
            }
        }
    }

    //sunset and sunrise UNIX timestamp --- ex. seconds since Jan 01 1970.
    fun setWeatherIcon(actualId: Int, sunrise: Long, sunset: Long): String {
        val id = actualId / 100
        var icon = ""
        if (actualId == 800) { //Group 800: Clear
            val currentTime = Date().time
            icon = if (currentTime in sunrise until sunset) { //currentTime >= sunrise && currentTime < sunset
                "&#xf00d;"
            } else {
                "&#xf02e;"
            }
        } else {
            when (id) {
                2 -> icon = "&#xf01e;" //Group 2xx: Thunderstorm
                3 -> icon = "&#xf01c;" //Group 3xx: Drizzle
                5 -> icon = "&#xf019;" //Group 5xx: Rain
                6 -> icon = "&#xf01b;" //Group 6xx: Snow
                7 -> icon = "&#xf014;" //Group 7xx: Atmosphere
                8 -> icon = "&#xf013;" //Group 80x: Clouds
            }
        }
        return icon
    }
}