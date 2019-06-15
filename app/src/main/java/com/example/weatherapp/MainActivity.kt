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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    lateinit var selectCity: TextView
    lateinit var cityField: TextView
    lateinit var detailsField: TextView
    lateinit var currentTemperatureField: TextView
    lateinit var humidity_field: TextView
    lateinit var pressure_field: TextView
    lateinit var weatherIcon: TextView
    lateinit var updatedField: TextView
    lateinit var loader: ProgressBar
    lateinit var weatherFont: Typeface
    var city : String = "Copenhagen" //default load if permission not granted
    val OPEN_WEATHER_MAP_API : String = "34910ce547d9588c2b86389c14d95e5c"
    lateinit var permissions : Array<String>
    lateinit var currentLocation: Location
    lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        loader = findViewById<View>(R.id.loader) as ProgressBar
        selectCity = findViewById<View>(R.id.selectCity) as TextView
        cityField = findViewById<View>(R.id.city_field) as TextView
        updatedField = findViewById<View>(R.id.updated_field) as TextView
        detailsField = findViewById<View>(R.id.details_field) as TextView
        currentTemperatureField = findViewById<View>(R.id.current_temperature_field) as TextView
        humidity_field = findViewById<View>(R.id.humidity_field) as TextView
        pressure_field = findViewById<View>(R.id.pressure_field) as TextView
        weatherIcon = findViewById<View>(R.id.weather_icon) as TextView
        weatherFont = Typeface.createFromAsset(assets, "fonts/weathericons-regular-webfont.ttf")
        weatherIcon.typeface = weatherFont
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

            alertDialog.setPositiveButton(
                "Change"
            ) { _ : DialogInterface, _ : Int ->
                city = input.text.toString()
                loadCityWeather(city)
            }
            alertDialog.setNegativeButton("Cancel"
            ) { dialog : DialogInterface, _ : Int -> dialog.cancel() }
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
            getLocation()
        }
    }
    fun loadLocationWeather(location: Location) {
        if (Function.isNetworkAvailable(applicationContext)) {
            val task = getWeatherLocation()
            task.execute(location.latitude.toString(), location.longitude.toString())
        } else {
            Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_LONG).show()
        }
    }
    fun getLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return
        }
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.setCostAllowed(false) //cannot find kotlin synthetic prop
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION
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
    // do operation background thread, update on UI/Main thread
    inner class getWeatherLocation : AsyncTask<String, Void, String>() { //<background, type to define progress, return from background - postExecute>
        //before any loads - initialsetup
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
        }
        //heavy to do in background thread
        override fun doInBackground(vararg args : String): String? { //varag as many of String

            return Function.executeGet("http://api.openweathermap.org/data/2.5/weather?lat=${args[0]}&lon=${args[1]}&appid=${OPEN_WEATHER_MAP_API}&units=metric")
        }

        //publish to UI - runs on UI thread so can access
        override fun onPostExecute(xml: String) {
            try {
                val json = JSONObject(xml)
                if (json != null) {
                    val details = json.getJSONArray("weather").getJSONObject(0)
                    val main = json.getJSONObject("main")
                    val df = DateFormat.getDateTimeInstance()
                    cityField.text =
                        json.getString("name").toUpperCase(Locale.US) + ", " + json.getJSONObject("sys").getString("country")
                    detailsField.text = details.getString("description").toUpperCase(Locale.US)
                    currentTemperatureField.text = String.format("%.2f", main.getDouble("temp")) + "°"
                    humidity_field.text = "Humidity: " + main.getString("humidity") + "%"
                    pressure_field.text = "Pressure: " + main.getString("pressure") + " hPa"
                    updatedField.text = df.format(Date(json.getLong("dt") * 1000))
                    weatherIcon.text = Html.fromHtml(
                        Function.setWeatherIcon(
                            details.getInt("id"),
                            json.getJSONObject("sys").getLong("sunrise") * 1000,
                            json.getJSONObject("sys").getLong("sunset") * 1000
                        )
                    )

                    loader.visibility = View.GONE
                }
            } catch (e: JSONException) {
                Toast.makeText(applicationContext, "Error! Perhaps you've mispelled the city?", Toast.LENGTH_SHORT).show()
                loader.visibility = View.GONE
            }
        }
    }
    override fun onProviderEnabled(provider: String?) {
        TODO("not implemented")
    }

    override fun onProviderDisabled(provider: String?) {
        TODO("not implemented")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        TODO("not implemented")
    }


    companion object {
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    }

    fun loadCityWeather(query: String) {
        if (Function.isNetworkAvailable(applicationContext)) {
            val task = getWeatherCity()
            task.execute(query)
        } else {
            Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_LONG).show()
        }
    }
    // do operation background thread, update on UI/Main thread
    inner class getWeatherCity : AsyncTask<String, Void, String>() { //<background, type to define progress, return from background - postExecute>
        //before any loads - initialsetup
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
        }
        //heavy to do in background thread
        override fun doInBackground(vararg args : String): String? { //varag as many of String
            return Function.executeGet("http://api.openweathermap.org/data/2.5/weather?q=" + args[0] +
                    "&units=metric&appid=" + OPEN_WEATHER_MAP_API
            )
        }

        //publish to UI - runs on UI thread so can access
        override fun onPostExecute(xml: String) {
            try {
                val json = JSONObject(xml)
                if (json != null) {
                    val details = json.getJSONArray("weather").getJSONObject(0)
                    val main = json.getJSONObject("main")
                    val df = DateFormat.getDateTimeInstance()
                    cityField.text =
                        json.getString("name").toUpperCase(Locale.US) + ", " + json.getJSONObject("sys").getString("country")
                    detailsField.text = details.getString("description").toUpperCase(Locale.US)
                    currentTemperatureField.text = String.format("%.2f", main.getDouble("temp")) + "°"
                    humidity_field.text = "Humidity: " + main.getString("humidity") + "%"
                    pressure_field.text = "Pressure: " + main.getString("pressure") + " hPa"
                    updatedField.text = df.format(Date(json.getLong("dt") * 1000))
                    weatherIcon.text = Html.fromHtml(
                        Function.setWeatherIcon( details.getInt("id"),
                            json.getJSONObject("sys").getLong("sunrise") * 1000,
                            json.getJSONObject("sys").getLong("sunset") * 1000))
                    loader.visibility = View.GONE
                }
            } catch (e: JSONException) {
                Toast.makeText(applicationContext, "Error! Perhaps you've mispelled the city?", Toast.LENGTH_SHORT).show()
                loader.visibility = View.GONE
            }
        }
    }
}