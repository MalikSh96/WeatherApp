package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import com.github.kittinunf.fuel.httpGet

object HTTP {

    fun isNetworkAvailable(context: Context): Boolean {
        return (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo != null
    }

    /**
     * @param targetUrl the WeatherAPI URL to perform a HTTP exchange to
     *
     * performs the actual communication between client and API
     *
     * @return a String containing JSON weather data for the given location
     */
    fun executeGet(targetURL: String): String? {
        val (request, response, result) = targetURL.httpGet().responseString() // result is Result<String, FuelError>
        if (response.statusCode != 200) return ""
        return result.get()
    }
}