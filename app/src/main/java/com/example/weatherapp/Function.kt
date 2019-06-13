package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object Function {

    fun isNetworkAvailable(context: Context): Boolean {
        return (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo != null
    }

    /*fun executeGet(targetURL: String): String? {
        val url: URL
        var connection: HttpURLConnection? = null
        try {
            //Create connection
            url = URL(targetURL)
            connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("content-type", "application/json;  charset=utf-8")
            connection.setRequestProperty("Content-Language", "en-US")
            connection.useCaches = false
            connection.doInput = true
            connection.doOutput = false

            val `is`: InputStream
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK)
                `is` = connection.errorStream
            else
                `is` = connection.inputStream
            val rd = BufferedReader(InputStreamReader(`is`))
            var line: String
            val response = StringBuffer()
            line = rd.readLine()
            while (line != null) {
                response.append(line)
                response.append('\r')
            }
            rd.close()
            return response.toString()
        } catch (e: Exception) {
            return null
        } finally {
            connection?.disconnect()
        }
    }*/

    fun executeGet(targetURL: String): String? {
        val url: URL
        /*lateinit*/ var connection: HttpURLConnection? = null
        try {
            //Create connection
            url = URL(targetURL)
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
            //issue when converting is here
            //https://stackoverflow.com/questions/52018289/java-lang-outofmemoryerror-failed-to-allocate-a-267911176-byte-allocation-with
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

    fun setWeatherIcon(actualId: Int, sunrise: Long, sunset: Long): String {
        val id = actualId / 100
        var icon = ""
        if (actualId == 800) {
            val currentTime = Date().time
            icon = if (currentTime >= sunrise && currentTime < sunset) {
                "&#xf00d;"
            } else {
                "&#xf02e;"
            }
        } else {
            when (id) {
                2 -> icon = "&#xf01e;"
                3 -> icon = "&#xf01c;"
                7 -> icon = "&#xf014;"
                8 -> icon = "&#xf013;"
                6 -> icon = "&#xf01b;"
                5 -> icon = "&#xf019;"
            }
        }
        return icon
    }
}
/*
fun executeGet(targetURL: String): String? {
        val url: URL
        /*lateinit*/ var connection: HttpURLConnection? = null
        try {
            //Create connection
            url = URL(targetURL)
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
            val line: String
            val response = StringBuffer()
            //issue when converting is here
            line = rd.readLine()
            while (line != null) {
                response.append(line)
                response.append('\r')
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
 */