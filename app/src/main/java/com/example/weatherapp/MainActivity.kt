package com.example.weatherapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.Html.fromHtml
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.weatherapp.helper.HtmlCompat
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
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
    var city : String = "Barcelona, ES"
    var OPEN_WEATHER_MAP_API : String = "34910ce547d9588c2b86389c14d95e5c"


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

        taskLoadUp(city)

        selectCity.setOnClickListener {
            print("In selectcity")
            val alertDialog : AlertDialog.Builder  = AlertDialog.Builder(this@MainActivity)
            alertDialog.setTitle("Change City")
            val input = EditText(this@MainActivity)
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
                taskLoadUp(city)
            }
            alertDialog.setNegativeButton("Cancel"
            ) { dialog : DialogInterface, _ : Int -> dialog.cancel() }
            alertDialog.show()
        }

    }

    fun taskLoadUp(query: String) {
        if (Function.isNetworkAvailable(applicationContext)) {
            val task = DownloadWeather()
            task.execute(query)
        } else {
            Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_LONG).show()
        }
    }

    inner class DownloadWeather : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg args : /*Array<String>*/ String): String? {
            return Function.executeGet("http://api.openweathermap.org/data/2.5/weather?q=" + args[0] +
                    "&units=metric&appid=" + OPEN_WEATHER_MAP_API
            )
        }

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
                Toast.makeText(applicationContext, "Error, Check City", Toast.LENGTH_SHORT).show()
            }
        }
    }
}