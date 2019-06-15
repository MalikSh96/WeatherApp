package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
        val url = URL(targetURL)
        var connection: HttpURLConnection? = url.openConnection() as HttpURLConnection //have to access in finally
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("content-type", "application/json;  charset=utf-8")
            connection.setRequestProperty("Content-Language", "en-US")
            connection.useCaches = false
            connection.doInput = true
            connection.doOutput = false

            val inpstr: InputStream
            val status = connection.responseCode

            inpstr = if (status != HttpURLConnection.HTTP_OK)
                connection.errorStream
            else
                connection.inputStream
            val rd = BufferedReader(InputStreamReader(inpstr))
            var line = rd.readLine()
            val response = StringBuffer()
            while (line != null) {
                response.append(line)
                response.append('\r')
                line = rd.readLine()
            }
            rd.close()
            return response.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }
}