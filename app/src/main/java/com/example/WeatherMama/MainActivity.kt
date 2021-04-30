package com.example.WeatherMama

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URL
import kotlin.math.round



class MainActivity : AppCompatActivity(), LocationListener {

    val CITY: String = "Toronto,CA"
    var latitude: Double = 42.8
    var longitude: Double = -8.0
    val API: String = "12f7eab7644e78873e2a5a0db47da7ca"


    private fun getLocation() {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 2)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }
    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        weathertask().execute()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getLocation()

        //Picasso.get().load("http://openweathermap.org/img/wn/10d@4x.png").into(findViewById<ImageView>(R.id.weatherIconContainer))
    }

    inner class weathertask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errortext).visibility = View.GONE
        }

        override fun doInBackground(vararg p0: String?): String {
            var response: String
            try {
                response = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$API").readText(Charsets.UTF_8)
            //response = URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API").readText(Charsets.UTF_8)
            } catch (e: Exception) {
                response = ""
            }
            return response
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt:Long = jsonObj.getLong("dt")
                //val updatedAtText = "Updated at: "+SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(updatedAt*1000)
                val temp = roundToInt(main.getString("temp"))+"°C"
                val feelsLike = "Feels like "+roundToInt(main.getString("feels_like"))+"°C"
                val tempMin = "Min Temp: "+roundToInt(main.getString("temp_min"))+"°C"
                val tempMax = "Max Temp: "+roundToInt((main.getString("temp_max")))+"°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val sunrise: String = sys.getString("sunrise")
                val sunset: String = sys.getString("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name")+", "+sys.getString("country")


                findViewById<TextView>(R.id.address).text = address
                //findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.feelsLike).text = feelsLike
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                //findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
                //findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
                //findViewById<TextView>(R.id.wind).text = windSpeed
                //findViewById<TextView>(R.id.pressure).text = pressure
                //findViewById<TextView>(R.id.humidity).text = humidity
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

                val bm = Picasso.get().load("http://openweathermap.org/img/wn/10d@4x.png").get()

                val bitmapd: BitmapDrawable = BitmapDrawable(bm)


                findViewById<ImageView>(R.id.weatherIconContainer).setImageDrawable(bitmapd)

           // Picasso.get().load("http://openweathermap.org/img/wn/10d@4x.png").into(findViewById<ImageView>(R.id.weatherIconContainer))

            }
            catch (e: Exception)
            {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<TextView>(R.id.errortext).visibility = View.VISIBLE
            }
        }







        fun roundToInt(text: String) : String {
            return round(text.toDouble()).toInt().toString()
        }

    }
    }
