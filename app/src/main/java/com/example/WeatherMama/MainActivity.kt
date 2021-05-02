package com.example.WeatherMama

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.net.URL
import kotlin.math.round

class MainActivity : AppCompatActivity() {//}, LocationListener {

    val CITY: String = "Toronto,CA"
    var latitude: Double = 42.8
    var longitude: Double = -8.0
    val API: String = "12f7eab7644e78873e2a5a0db47da7ca"
    var weatherIconURL: String = "" //Used to store the current icon URL and prevent it form downloading if we already have it.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var areaText: String = ""
    private var areaTextOpenWeather: String = ""


    //Define the temperature ranges.
    enum class TemperatureRange {
        ExtraCold, Cold, Chilly, Mild, Warm, Hot, Undefined
    }

    //Store the current temperature range in this variable.
    var temperatureRange: TemperatureRange = TemperatureRange.Undefined

    //List with the names of the icons used for the different temperature ranges. This lists of names are empty yet, call the function initVariables to initialize everything.
    private val iconsBaseLayer = ArrayList<String>()
    private val iconsSecondLayer = ArrayList<String>()
    private val iconsOuterLayer = ArrayList<String>()


    /**
     * Initializes all the variables. This method must be called from OnCreate
     */
    private fun initVariables() {
        iconsBaseLayer.add("bl5orless")
        iconsBaseLayer.add("bl5to10")
        iconsBaseLayer.add("bl10to15")
        iconsBaseLayer.add("bl15to20")
        iconsBaseLayer.add("bl20to25")
        iconsBaseLayer.add("bl25plus")

        iconsSecondLayer.add("sl5orless")
        iconsSecondLayer.add("sl5to10")
        iconsSecondLayer.add("sl10to15")
        iconsSecondLayer.add("sl15to20")
        iconsSecondLayer.add("sl20to25")
        iconsSecondLayer.add("sl25plus")

        iconsOuterLayer.add("ol5orless")
        iconsOuterLayer.add("ol5to10")
        iconsOuterLayer.add("ol10to15")
        iconsOuterLayer.add("ol15to20")
        iconsOuterLayer.add("ol20to25")
        iconsOuterLayer.add("ol25plus")


    }


    /**
     * Updates the temperature range and refreshes the icons shown if the range changes.
     * @param realFeel string containing the real feel. Unused for now.
     * @param currentTemperature string containing the current temperature.
     */
    private fun updateTemperatureRange(realFeel: String, currentTemperature: String) {

        var temp: Double = 0.0

        try {
            temp = currentTemperature.toDouble()    //Convert the string to a number.
        } catch (e: java.lang.Exception) {
            return
        }

        var thisTemperatureRange: TemperatureRange = TemperatureRange.Undefined

        if (temp > 25) thisTemperatureRange = TemperatureRange.Hot
        else if (temp > 20) thisTemperatureRange = TemperatureRange.Warm
        else if (temp > 15) thisTemperatureRange = TemperatureRange.Mild
        else if (temp > 10) thisTemperatureRange = TemperatureRange.Chilly
        else if (temp > 5) thisTemperatureRange = TemperatureRange.Cold
        else if (temp <= 5) thisTemperatureRange = TemperatureRange.ExtraCold
        else thisTemperatureRange = TemperatureRange.Undefined

        if (temperatureRange == thisTemperatureRange) return;
        temperatureRange = thisTemperatureRange
        if (temperatureRange == TemperatureRange.Undefined) return;

        //findViewById<ImageView>(R.id.innerlayerImageView).setImageResource(getImageId(this, iconsBaseLayer.elementAt(temperatureRange.ordinal)))
        //findViewById<ImageView>(R.id.secondlayerImageView).setImageResource(getImageId(this, iconsSecondLayer.elementAt(temperatureRange.ordinal)))
        //findViewById<ImageView>(R.id.outerlayerImageView).setImageResource(getImageId(this, iconsOuterLayer.elementAt(temperatureRange.ordinal)))
    }

    /**
     * Gets the id of a resource from its name.
     * @param context from where we call it.
     * @param imageName String containing the name of the file.
     * @returns The id number of that resource.
     */
    fun getImageId(context: Context, imageName: String): Int {
        return context.resources.getIdentifier("drawable/$imageName", null, context.packageName)
    }

    public override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions()
            }
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                var lastLocation = task.result
                latitude = (lastLocation)!!.latitude
                longitude = (lastLocation)!!.longitude
                weathertask().execute()
                reverseGeoCode().execute()

            } else {
                Log.w(TAG, "getLastLocation:exception", task.exception)
                showMessage("No location detected. Make sure location is enabled on the device.")
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar("Location permission is needed for core functionality", "Okay",
                    View.OnClickListener {
                        startLocationPermissionRequest()
                    })
        } else {
            startLocationPermissionRequest()
        }
    }

    private fun showMessage(string: String) {
        Toast.makeText(this@MainActivity, string, Toast.LENGTH_LONG).show()
    }

    private fun showSnackbar(
            mainTextStringId: String, actionStringId: String,
            listener: View.OnClickListener
    ) {
        Toast.makeText(this@MainActivity, mainTextStringId, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>,
            grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted.
                    getLastLocation()
                }
                else -> {
                    showSnackbar("Permission was denied", "Settings",
                            View.OnClickListener {
                                // Build intent that displays the App settings screen.
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts(
                                        "package",
                                        Build.DISPLAY, null
                                )
                                intent.data = uri
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = "LocationProvider"
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initVariables()
    }


    /**
     * Downloads an icon from openWeather according to the icon code and shows it in the interface.
     * @param code string containing the icon code
     */
    fun showWeatherIcon(code: String) {
        val weatherImageView: ImageView = findViewById<ImageView>(R.id.weatherIconContainer)
        val thisUrl = "https://openweathermap.org/img/wn/" + code + "@4x.png"
        if (thisUrl == weatherIconURL)
            return
        weatherIconURL = thisUrl
        if (weatherIconURL !== null) {
            Glide.with(this)
                    .load(weatherIconURL)
                    .into(weatherImageView)
        } else {
            weatherImageView.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    inner class weathertask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg p0: String?): String {
            var response: String
            try {
                response = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$API").readText(Charsets.UTF_8)
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

                val tempDouble = main.getString("temp")
                val temp = roundToInt(tempDouble) + "°C"
                val feelsLikeDouble = main.getString("feels_like")
                val feelsLike = "Feels like " + roundToInt(feelsLikeDouble) + "°C"
                val tempMin = "Min Temp: " + roundToInt(main.getString("temp_min")) + "°C"
                val tempMax = "Max Temp: " + roundToInt((main.getString("temp_max"))) + "°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val sunrise: String = sys.getString("sunrise")
                val sunset: String = sys.getString("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name") + ", " + sys.getString("country")
                val weatherIcon = weather.getString("icon")

                //findViewById<TextView>(R.id.address).text = address
                //findViewById<TextView>(R.id.updated_at).text = updatedAt
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.feelsLike).text = feelsLike
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                showWeatherIcon(weatherIcon)
                updateTemperatureRange(feelsLikeDouble, tempDouble)

                areaTextOpenWeather = address;

            } catch (e: Exception) {
                return
            }
        }
    }

    inner class reverseGeoCode() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg p0: String?): String {
            var response: String
            try {
                response = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&zoom=18&addressdetails=1&format=json").readText(Charsets.UTF_8)
            } catch (e: Exception) {
                response = ""
                areaText = areaTextOpenWeather
            }
            return response
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            findViewById<TextView>(R.id.address).text = "post!"
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result)
                val addr = jsonObj.getJSONObject("address")

                try {
                    val local = addr.getString("neighbourhood")
                    val city = addr.getString("city")
                    findViewById<TextView>(R.id.address).text = local + ", " + city
                    return
                } catch (e: java.lang.Exception) {
                }
                try {
                    val local = addr.getString("suburb")
                    val city = addr.getString("city")
                    findViewById<TextView>(R.id.address).text = local + ", " + city
                    return
                } catch (e: java.lang.Exception) {
                }
                try {
                    val local = addr.getString("town")
                    val city = addr.getString("city")
                    findViewById<TextView>(R.id.address).text = local + ", " + city
                    return
                } catch (e: java.lang.Exception) {
                }
                try {
                    val local = addr.getString("village")
                    val city = addr.getString("city")
                    findViewById<TextView>(R.id.address).text = local + ", " + city
                    return
                } catch (e: java.lang.Exception) {
                }
                try {
                    val local = addr.getString("hamlet")
                    val city = addr.getString("city")
                    findViewById<TextView>(R.id.address).text = local + ", " + city
                    return
                } catch (e: java.lang.Exception) {
                }
                areaText = areaTextOpenWeather
                findViewById<TextView>(R.id.address).text = areaText
            } catch (e: Exception) {
                areaText = areaTextOpenWeather
                findViewById<TextView>(R.id.address).text = "Excp!"
            }
        }
    }

    /**
     * Rounds a number to the closest integer.
     * @param text string containing a number (any type).
     * @return a string containing the number in text rounded to the closes integer.
     */
    fun roundToInt(text: String): String {
        return round(text.toDouble()).toInt().toString()
    }
}
