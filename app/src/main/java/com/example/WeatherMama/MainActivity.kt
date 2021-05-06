package com.example.WeatherMama

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {
    //Global vars used in the entire class.
    var latitude: Double = 42.8
    var longitude: Double = -8.0
    private val API: String = "12f7eab7644e78873e2a5a0db47da7ca"
    private var weatherIconURL: String = "" //Used to store the current icon URL and prevent it form downloading if we already have it.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var areaTextOpenWeather: String = ""
    // Instantiate the RequestQueue used for Json requests.
    private var queue : RequestQueue? = null


    //Define the temperature ranges.
    enum class TemperatureRange { //enum assigns numeric values from 0 to 6 to the weather statuses below.
        ExtraCold, Cold, Chilly, Mild, Warm, Hot, Undefined
    }

    //Store the current temperature range in this variable. We use "Undefined" by default.
    private var temperatureRange: TemperatureRange = TemperatureRange.Undefined

    //List with the names of the icons used for the different temperature ranges. This lists of names are empty yet, call the function initVariables to initialize everything.
    private val iconsBaseLayer = ArrayList<String>()
    private val iconsSecondLayer = ArrayList<String>()
    private val iconsOuterLayer = ArrayList<String>()


    private val requestingLocationUpdates : Boolean = true
    private lateinit var locationCallback: LocationCallback
    private var locationRequest : LocationRequest = LocationRequest.create()


    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    /**
     * Initializes the 3 variables above. Must be listed in the same order as ExtraCold,..., Undefined. This method must be called from OnCreate.
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

        queue = Volley.newRequestQueue(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    longitude = location.longitude
                    latitude = location.latitude
                    requestWeather()
                    requestGeoCode()
                    requestWeatherHourly()
                }
            }
        }
    }


    /**
     * Updates the temperature range and refreshes the icons shown if the range changes.
     * @param realFeel string containing the real feel. Unused for now.
     * @param currentTemperature string containing the current temperature.
     */
    private fun updateTemperatureRange(realFeel: String, currentTemperature: String) {

        var temp: Double

        try {
            temp = currentTemperature.toDouble()    //Convert the string to a number.
        } catch (e: java.lang.Exception) {
            return
        }

        var thisTemperatureRange: TemperatureRange

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
     * Gets the id of a resource (icons, in this case) from its name.
     * @param context from where we call it.
     * @param imageName String containing the name of the file.
     * @returns The id number of that resource.
     */
    fun getImageId(context: Context, imageName: String): Int {
        return context.resources.getIdentifier("drawable/$imageName", null, context.packageName)
    }

    //Launches layout and initializes variables at the beginning.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initVariables()

        if (ContextCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION) !==
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }
        getLastLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                                    Manifest.permission.ACCESS_FINE_LOCATION) ===
                                    PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }




    //Request a GPS location in the background and, when successful, stores latitude, longitude, and executes.
    private fun getLastLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        requestWeather()
                        requestGeoCode()
                        requestWeatherHourly()
                    }
                    //We didn't have any lastlocation saved, get a new location.
                    else {
                        //We will pull the data from the gps.
                    }
                }
    }


    /**
     * Downloads an icon from openWeather according to the icon code and shows it in the interface.
     * @param code string containing the icon code
     */
    private fun showWeatherIcon(code: String) {
        val weatherImageView: ImageView = findViewById<ImageView>(R.id.weatherIconContainer)
        val thisUrl = "https://openweathermap.org/img/wn/" + code + "@4x.png"
        if (thisUrl == weatherIconURL)
            return
        weatherIconURL = thisUrl
        Glide.with(this)
                .load(weatherIconURL)
                .into(weatherImageView)
    }

    /**
     * Downloads an icon from openWeather according to the icon code and shows it in the interface.
     * @param code string containing the icon code
     * @param iconResource Int containing the resource
     * @param iconQuality 4 for best quality, 2 medium, 1 low.
     */
    private fun showWeatherIcon(code: String, iconResource: Int, iconQuality: Int) {
        val view: ImageView = findViewById<ImageView>(iconResource)
        val thisUrl = "https://openweathermap.org/img/wn/" + code + "@" + iconQuality.toString() + "x.png"

        Glide.with(this)
                .load(thisUrl)
                .into(view)
    }


    private fun requestWeather() {
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$API"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                { response ->
                    val jsonObj = JSONObject(response)
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                   // val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                    val tempDouble = main.getString("temp")
                    val temp = roundToInt(tempDouble) + "°C"
                    val feelsLikeDouble = main.getString("feels_like")
                    val feelsLike = "Feels like " + roundToInt(feelsLikeDouble) + "°C"
                    val weatherDescription = weather.getString("description")
                    val address = jsonObj.getString("name") + ", " + sys.getString("country")
                    val weatherIcon = weather.getString("icon")

                    findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                    findViewById<TextView>(R.id.feelsLike).text = feelsLike
                    findViewById<TextView>(R.id.temp).text = temp

                    showWeatherIcon(weatherIcon)
                    updateTemperatureRange(feelsLikeDouble, tempDouble)

                    areaTextOpenWeather = address;
                },
                Response.ErrorListener {
                    return@ErrorListener
                })

        // Add the request to the RequestQueue.
        queue!!.add(stringRequest)
    }

    private fun requestGeoCode() {

        val url = "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&zoom=18&addressdetails=1&format=json"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                { response ->

                    var replyJson = JSONObject(response)
                    val add = replyJson.getJSONObject("address")
                    try {
                        val local = add.getString("neighbourhood")
                        val city = add.getString("city")
                        findViewById<TextView>(R.id.address).text = local + ", " + city
                        return@StringRequest
                    } catch (e: java.lang.Exception) {
                    }

                    try {
                        val local = add.getString("suburb")
                        val city = add.getString("city")
                        findViewById<TextView>(R.id.address).text = local + ", " + city
                        return@StringRequest
                    } catch (e: java.lang.Exception) {
                    }

                    try {
                        val local = add.getString("town")
                        val city = add.getString("city")
                        findViewById<TextView>(R.id.address).text = local + ", " + city
                        return@StringRequest
                    } catch (e: java.lang.Exception) {
                    }

                    try {
                        val local = add.getString("village")
                        val city = add.getString("city")
                        findViewById<TextView>(R.id.address).text = local + ", " + city
                        return@StringRequest
                    } catch (e: java.lang.Exception) {
                    }

                    try {
                        val local = add.getString("hamlet")
                        val city = add.getString("city")
                        findViewById<TextView>(R.id.address).text = local + ", " + city
                        return@StringRequest
                    } catch (e: java.lang.Exception) {
                    }

                    try {
                        val city = add.getString("city")
                        findViewById<TextView>(R.id.address).text = city
                        return@StringRequest
                    } catch (e: java.lang.Exception) {
                    }
                    findViewById<TextView>(R.id.address).text = areaTextOpenWeather
                },
                {
                    findViewById<TextView>(R.id.address).text = areaTextOpenWeather
                })

        // Add the request to the RequestQueue.
        queue!!.add(stringRequest)
    }



    private fun requestWeatherHourly() {
        val url = "https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&exclude=current,minutely,daily,alerts&units=metric&appid=$API"

        val temperatures = arrayListOf<String>();
        val hours = arrayListOf<String>();
        val icons = arrayListOf<String>();
        val realFeels = arrayListOf<String>();
        val minutes = arrayListOf<String>();
        val addressDescriptions = arrayListOf<String>();


        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                { response ->
                    val main = JSONObject(response)
                    val array = main.getJSONArray("hourly")
                    for (i in 0 until array.length()) {
                        val thisHour = array.getJSONObject(i)

                        //Time in UTC
                        var utcTimeRaw = thisHour.getString("dt")
                        val temp = roundToInt(thisHour.getString("temp")) + "°C"
                        val realFeel = roundToInt(thisHour.getString("feels_like")) + "°C"
                        val weather = thisHour.getJSONArray("weather").getJSONObject(0)
                        //val weatherDescription = weather.getString("description")
                        val weatherIcon = weather.getString("icon")


                        val hour = Date(utcTimeRaw.toLong() * 1000).hours.toString()
                        val minute = Date(utcTimeRaw.toLong() * 1000).minutes.toString()
                        //val address = thisHour.getString("name")

                        temperatures.add(temp)
                        hours.add(hour)
                        icons.add(weatherIcon)
                        realFeels.add(realFeel)
                        minutes.add(minute)
                        addressDescriptions.add("")

                    }
                },
                Response.ErrorListener {
                    return@ErrorListener
                })

        // Add the request to the RequestQueue.
        queue!!.add(stringRequest)

        //Start the icons for hourly weather request.
        var recyclerView = findViewById<RecyclerView>(R.id.recycler_view)



        val adapter = CustomAdapter(this, temperatures, realFeels, addressDescriptions, icons, hours, minutes, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL ,false)
    }



    /**
     * Rounds a number to the closest integer.
     * @param text string containing a number (any type).
     * @return a string containing the number in text rounded to the closes integer.
     */
    fun roundToInt(text: String): String {
        return round(text.toDouble()).toInt().toString()
    }


    public fun updateValues (values : HourlyWeather) {
        //findViewById<TextView>(R.id.status).text = values.weatherDescription.capitalize()
        findViewById<TextView>(R.id.feelsLike).text = values.realFeel
        findViewById<TextView>(R.id.temp).text = values.temperature

        showWeatherIcon(values.iconName)
        updateTemperatureRange(values.realFeel, values.temperature)
        areaTextOpenWeather = values.addressDescription;
    }

    class HourlyWeather {
        var temperature :String = ""
        var realFeel :String = ""
        var addressDescription :String = ""
        var iconName : String = ""
        var hour : String = ""
        var position : Int = 0
        var minutes : String = ""
        lateinit var mainAct : MainActivity

        lateinit var imageViewMainIcon : ImageView
        lateinit var textViewTemp : TextView
        lateinit var textViewHour : TextView

        constructor(temperature : String,
                    realFeel : String,
                    addressDescription :String,
                    iconName : String,
                    hour : String,
                    position : Int,
                    minutes : String,
                    imageViewMainIcon : ImageView,
                    mainAct : MainActivity
        ) {
            this.temperature = temperature
            this.realFeel = realFeel
            this.addressDescription = addressDescription
            this.iconName = iconName
            this.hour = hour
            this.position = position
            this.minutes = minutes
            this.imageViewMainIcon = imageViewMainIcon

            this.imageViewMainIcon .setOnClickListener { view ->
                mainAct.updateValues(this)
            }
            this.imageViewMainIcon .isClickable = true
        }
    }



    class CustomAdapter(
            private val context: Context,
            private val temperatures : java.util.ArrayList<String>,
            private val realFeels: java.util.ArrayList<String>,
            private val addressDescriptions: java.util.ArrayList<String>,
            private val iconNames: java.util.ArrayList<String>,
            private val hours: java.util.ArrayList<String>,
            private val minutes: java.util.ArrayList<String>,
            private val mainAct : MainActivity
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        lateinit var time: TextView
        lateinit var temp: TextView
        lateinit var icon: ImageView

        var listHourlyWeather = arrayListOf<HourlyWeather>()

        private inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

            init {
                time = itemView.findViewById(R.id.time)
                temp = itemView.findViewById(R.id.temp)
                icon = itemView.findViewById(R.id.icon)
            }

            internal fun bind(position: Int) {

                listHourlyWeather.add(HourlyWeather(
                        temperatures[position],
                        realFeels[position],
                        addressDescriptions[position],
                        iconNames[position],
                        hours[position],
                        position,
                        minutes[position], icon, mainAct))



                val thisUrl = "https://openweathermap.org/img/wn/" + iconNames[position] + "@2x.png"
                Glide.with(context)
                        .load(thisUrl)
                        .into(icon)
                time.text = hours[position]
                temp.text = temperatures[position]



            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.hourly, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as ViewHolder).bind(position)
        }

        override fun getItemCount(): Int {
            return temperatures.size
        }
    }





}
