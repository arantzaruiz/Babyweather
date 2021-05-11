package com.example.WeatherMama

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round


class MainActivity : AppCompatActivity() {

    //Global vars used in the entire class.
    var latitude: Double = 42.8
    var longitude: Double = -8.0
    private val API: String = "12f7eab7644e78873e2a5a0db47da7ca"
    private var weatherIconURL: String =
        "" //Used to store the current icon URL and prevent it form downloading if we already have it.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var areaTextOpenWeather: String = ""

    // Instantiate the RequestQueue used for Json requests.
    private var queue: RequestQueue? = null

    lateinit var geocoder: Geocoder
    var locations: String = ""

    lateinit var latitudes: MutableList<Double>
    lateinit var longitudes: MutableList<Double>

    //Define the temperature ranges.
    enum class TemperatureRange { //enum assigns numeric values from 0 to 6 to the weather statuses below.
        ExtraCold, Cold, Chilly, Mild, Warm, Hot, Undefined
    }

    //Store the current temperature range in this variable. We use "Undefined" by default.
    private var temperatureRange: TemperatureRange = TemperatureRange.Undefined

    //List with the names of the icons used for the different temperature ranges. This lists of names are empty yet, call the function initVariables to initialize everything.
    private val iconsInnerLayerTop = ArrayList<String>()
    private val iconsInnerLayerBottom = ArrayList<String>()
    private val iconsSecondLayerTop = ArrayList<String>()
    private val iconsSecondLayerBottom = ArrayList<String>()
    private val iconsOuterLayerTop = ArrayList<String>()
    private val iconsOuterLayerBottom = ArrayList<String>()

    private lateinit var iltop: ImageView
    private lateinit var ilbottom: ImageView
    private lateinit var slbottom: ImageView
    private lateinit var sltop: ImageView
    private lateinit var oltop: ImageView
    private lateinit var olbottom: ImageView

    private val requestingLocationUpdates: Boolean = true
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) {
            startLocationUpdates()
        }
    }

    /**
     * Launches GPS in background for Android 10.
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
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


        locationRequest = LocationRequest.create().apply {
            interval = java.util.concurrent.TimeUnit.SECONDS.toMillis(60)
            fastestInterval = java.util.concurrent.TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = java.util.concurrent.TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        iconsInnerLayerTop.add("il5lesstop")
        iconsInnerLayerTop.add("il5to10top")
        iconsInnerLayerTop.add("il10to15top")
        iconsInnerLayerTop.add("il15to20top")
        iconsInnerLayerTop.add("il20to25top")
        iconsInnerLayerTop.add("il25plustop")

        iconsInnerLayerBottom.add("il5lessbottom")
        iconsInnerLayerBottom.add("il5to10bottom")
        iconsInnerLayerBottom.add("il10to15bottom")
        iconsInnerLayerBottom.add("il15to20bottom")
        iconsInnerLayerBottom.add("il20to25bottom")
        iconsInnerLayerBottom.add("il25plusbottom")

        iconsSecondLayerTop.add("sl5lesstop")
        iconsSecondLayerTop.add("sl5to10top")
        iconsSecondLayerTop.add("sl10to15top")
        iconsSecondLayerTop.add("sl15to20top")
        iconsSecondLayerTop.add("sl20to25top")
        iconsSecondLayerTop.add("sl25plustop")

        iconsSecondLayerBottom.add("sl5lessbottom")
        iconsSecondLayerBottom.add("sl5to10bottom")
        iconsSecondLayerBottom.add("sl10to15bottom")
        iconsSecondLayerBottom.add("sl15to20bottom")
        iconsSecondLayerBottom.add("sl20to25bottom")
        iconsSecondLayerBottom.add("sl25plusbottom")

        iconsOuterLayerTop.add("ol5lesstop")
        iconsOuterLayerTop.add("ol5to10top")
        iconsOuterLayerTop.add("ol10to15top")
        iconsOuterLayerTop.add("ol15to20top")
        iconsOuterLayerTop.add("ol20to25top")
        iconsOuterLayerTop.add("ol25plustop")

        iconsOuterLayerBottom.add("ol5lessbottom")
        iconsOuterLayerBottom.add("ol5to10bottom")
        iconsOuterLayerBottom.add("ol10to15bottom")
        iconsOuterLayerBottom.add("ol15to20bottom")
        iconsOuterLayerBottom.add("ol20to25bottom")
        iconsOuterLayerBottom.add("ol25plusbottom")

        iltop = findViewById<ImageView>(R.id.iltopimgview)
        ilbottom = findViewById<ImageView>(R.id.ilbottomimgview)
        sltop = findViewById<ImageView>(R.id.sltopimgview)
        slbottom = findViewById<ImageView>(R.id.slbottomimgview)
        oltop = findViewById<ImageView>(R.id.oltopimgview)
        olbottom = findViewById<ImageView>(R.id.olbottomimgview)

        latitudes = mutableListOf()
        longitudes = mutableListOf()

        queue = Volley.newRequestQueue(this)

        go2MainLayout()

        //Button that starts the location input:
        findViewById<Button>(R.id.launchLocationInput).setOnClickListener {
            go2LocationLayout()
        }
        findViewById<Button>(R.id.LocationBack).setOnClickListener {
            go2MainLayout()
        }
        findViewById<TextView>(R.id.Location0).setOnClickListener {
            if (findViewById<TextView>(R.id.Location0).text == "") return@setOnClickListener;
            go2MainLayout()
            latitude = latitudes[0]
            longitude = longitudes[0]
            requestWeather()
            requestGeoCode()
            requestWeatherHourly()
        }
        findViewById<TextView>(R.id.Location1).setOnClickListener {
            if (findViewById<TextView>(R.id.Location1).text == "") return@setOnClickListener;
            go2MainLayout()
            latitude = latitudes[1]
            longitude = longitudes[1]
            requestWeather()
            requestGeoCode()
            requestWeatherHourly()
        }
        findViewById<TextView>(R.id.Location2).setOnClickListener {
            if (findViewById<TextView>(R.id.Location2).text == "") return@setOnClickListener;
            go2MainLayout()
            latitude = latitudes[2]
            longitude = longitudes[2]
            requestWeather()
            requestGeoCode()
            requestWeatherHourly()
        }
        findViewById<TextView>(R.id.Location3).setOnClickListener {
            if (findViewById<TextView>(R.id.Location3).text == "") return@setOnClickListener;
            go2MainLayout()
            latitude = latitudes[3]
            longitude = longitudes[3]
            requestWeather()
            requestGeoCode()
            requestWeatherHourly()
        }
        findViewById<TextView>(R.id.Location4).setOnClickListener {
            if (findViewById<TextView>(R.id.Location4).text == "") return@setOnClickListener;
            go2MainLayout()
            latitude = latitudes[4]
            longitude = longitudes[4]
            requestWeather()
            requestGeoCode()
            requestWeatherHourly()
        }

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                updateLocations()
                mainHandler.postDelayed(this, 1000)
            }
        })

        findViewById<EditText>(R.id.editLocationText).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                locations = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                var currentLocation = locationResult.lastLocation
                longitude = currentLocation.longitude
                latitude = currentLocation.latitude
                requestWeather()
                requestGeoCode()
                requestWeatherHourly()
            }
        }
    }

    var lastLocations: String = ""

    /**
     * Returns API locations upon typing the initial location letters using the nominatim API.
     */
    private fun updateLocations() {

        if (lastLocations == locations) return
        lastLocations = locations

        findViewById<TextView>(R.id.Location0).text = ""
        findViewById<TextView>(R.id.Location1).text = ""
        findViewById<TextView>(R.id.Location2).text = ""
        findViewById<TextView>(R.id.Location3).text = ""
        findViewById<TextView>(R.id.Location4).text = ""

        //Get the values, add the descriptions and the callbacks to the textviews.
        if (locations == "") return
        if (latitudes.isNotEmpty()) latitudes.clear()
        if (longitudes.isNotEmpty()) longitudes.clear()

        var loc = TextUtils.htmlEncode(locations)
        val url =
            "https://nominatim.openstreetmap.org/search?q=$loc&format=json"
        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val array = JSONArray(response)

                for (i in 0 until array.length()) {
                    val thisAddress = array.getJSONObject(i)

                    val address = thisAddress.getString("display_name")
                    val longS = thisAddress.getString("lon")
                    val latS = thisAddress.getString("lat")

                    longitudes.add(longS.toDouble())
                    latitudes.add(latS.toDouble())

                    if (i == 0) findViewById<TextView>(R.id.Location0).text = address
                    else if (i == 1) findViewById<TextView>(R.id.Location1).text = address
                    else if (i == 2) findViewById<TextView>(R.id.Location2).text = address
                    else if (i == 3) findViewById<TextView>(R.id.Location3).text = address
                    else findViewById<TextView>(R.id.Location4).text = address

                }
            },
            Response.ErrorListener {
                return@ErrorListener
            })

        // Add the request to the RequestQueue.
        queue!!.add(stringRequest)
    }

    /**
     * Goes to the main layout.
     */
    private fun go2MainLayout() {
        findViewById<RelativeLayout>(R.id.MainLayout).isVisible = true;
        findViewById<RelativeLayout>(R.id.MainLayout).isFocusable = true;
        findViewById<LinearLayout>(R.id.InputLocationLayout).isVisible = false;
        findViewById<LinearLayout>(R.id.InputLocationLayout).isFocusable = false;
        findViewById<EditText>(R.id.editLocationText).inputType = InputType.TYPE_NULL;
        val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }

    /**
     * Changes the layout to the frame layout to input text
     */
    private fun go2LocationLayout() {
        findViewById<RelativeLayout>(R.id.MainLayout).isVisible = false;
        findViewById<RelativeLayout>(R.id.MainLayout).isFocusable = false;
        findViewById<LinearLayout>(R.id.InputLocationLayout).isVisible = true;
        findViewById<LinearLayout>(R.id.InputLocationLayout).isFocusable = true;
        findViewById<EditText>(R.id.editLocationText).inputType = InputType.TYPE_CLASS_TEXT;
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

        iltop.setImageResource(
            getImageId(
                this,
                iconsInnerLayerTop.elementAt(temperatureRange.ordinal)
            )
        )
        ilbottom.setImageResource(
            getImageId(
                this,
                iconsInnerLayerBottom.elementAt(temperatureRange.ordinal)
            )
        )
        sltop.setImageResource(
            getImageId(
                this,
                iconsSecondLayerTop.elementAt(temperatureRange.ordinal)
            )
        )
        slbottom.setImageResource(
            getImageId(
                this,
                iconsSecondLayerBottom.elementAt(temperatureRange.ordinal)
            )
        )
        oltop.setImageResource(
            getImageId(
                this,
                iconsOuterLayerTop.elementAt(temperatureRange.ordinal)
            )
        )
        olbottom.setImageResource(
            getImageId(
                this,
                iconsOuterLayerBottom.elementAt(temperatureRange.ordinal)
            )
        )
    }

    /**
     * Gets the id of a resource (icons, in this case) from its name.
     * @param context from where we call it.
     * @param imageName String containing the name of the file.
     * @returns The id number of that resource.
     */
    private fun getImageId(context: Context, imageName: String): Int {
        return context.resources.getIdentifier("drawable/$imageName", null, context.packageName)
    }

    //Launches layout and initializes variables at the beginning.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initVariables()

        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !==
            PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                )
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                )
            }
        }
        getLastLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) ===
                                PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }


    /**
     * Request a GPS location in the background and, when successful, stores latitude, longitude, and executes.
     */
    private fun getLastLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
                    //We will pull the data from the gps. Requested in parallel
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
        val thisUrl =
            "https://openweathermap.org/img/wn/" + code + "@" + iconQuality.toString() + "x.png"

        Glide.with(this)
            .load(thisUrl)
            .into(view)
    }

    /**
     * Requests the weather at the gps locations stores in the global variables longitude, latitude. Parses the results and extracts the icon, temperature, realfeel, name of the location and description of the weather.
     */
    private fun requestWeather() {

        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$API"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val jsonObj = JSONObject(response)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
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


    /**
     * Requests the detailed name of a pair of gps coordinates using the nominatim reverse geocode api.
     */
    private fun requestGeoCode() {

        val url =
            "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&zoom=18&addressdetails=1&format=json"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val add: JSONObject
                var replyJson = JSONObject(response)
                try {
                    add = replyJson.getJSONObject("address")
                } catch (e: java.lang.Exception) {
                    throw e
                }
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

    /**
     * Requests the hourly weather (48 hours) at a given pair of gps coordinates, stored in the variables latitude and longitude and parses the relevant info.
     */
    private fun requestWeatherHourly() {
        val url =
            "https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&exclude=current,minutely,daily,alerts&units=metric&appid=$API"

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
                    //val sys = thisHour.getJSONObject("sys")

                    //Time in UTC
                    var utcTimeRaw = thisHour.getString("dt")
                    val tempDouble = thisHour.getString("temp")
                    val temp = roundToInt(tempDouble) + "°C"
                    val realFeelDouble = thisHour.getString("feels_like")
                    val realFeel = roundToInt(realFeelDouble) + "°C"
                    val weather = thisHour.getJSONArray("weather").getJSONObject(0)
                    //val weatherDescription = weather.getString("description")
                    val weatherIcon = weather.getString("icon")
                //    val address = thisHour.getString("name") + ", " + sys.getString("country")


                    val hour = Date(utcTimeRaw.toLong() * 1000).hours.toString()
                    val minute = Date(utcTimeRaw.toLong() * 1000).minutes.toString()
                    //val address = thisHour.getString("name")

                    temperatures.add(temp)
                    hours.add(hour)
                    icons.add(weatherIcon)
                    realFeels.add(realFeel)
                    minutes.add(minute)
                    addressDescriptions.add("")

                   // areaTextOpenWeather = address;

                    if (i==0) {
                        showWeatherIcon(weatherIcon)
                        updateTemperatureRange(realFeelDouble, tempDouble)
                    }

                }
            },
            Response.ErrorListener {
                return@ErrorListener
            })

        // Add the request to the RequestQueue.
        queue!!.add(stringRequest)

        //Start the icons for hourly weather request.
        var recyclerView = findViewById<RecyclerView>(R.id.recycler_view)


        val adapter = CustomAdapter(
            this,
            temperatures,
            realFeels,
            addressDescriptions,
            icons,
            hours,
            minutes,
            this
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter.notifyDataSetChanged();
    }


    /**
     * Rounds a number to the closest integer.
     * @param text string containing a number (any type).
     * @return a string containing the number in text rounded to the closes integer.
     */
    fun roundToInt(text: String): String {
        return round(text.toDouble()).toInt().toString()
    }

    /**
     * Updates the realfeel and temperature values shown in the UI with the given hpurly weather values.
     * @param values HourlyWeather containing all weather values at that given time.
     */
    public fun updateValues(values: HourlyWeather) {
        //findViewById<TextView>(R.id.status).text = values.weatherDescription.capitalize()
        findViewById<TextView>(R.id.feelsLike).text = values.realFeel
        findViewById<TextView>(R.id.temp).text = values.temperature

        showWeatherIcon(values.iconName)
        updateTemperatureRange(values.realFeel, values.temperature)
        areaTextOpenWeather = values.addressDescription;
    }

    /**
    * This class is used in an array to store weather, location and time information for every hour from the OpenWeather app.
     */
    class HourlyWeather {
        var temperature: String = ""
        var realFeel: String = ""
        var addressDescription: String = ""
        var iconName: String = ""
        var hour: String = ""
        var position: Int = 0
        var minutes: String = ""
        lateinit var mainAct: MainActivity

        lateinit var imageViewMainIcon: ImageView
        lateinit var textViewTemp: TextView
        lateinit var textViewHour: TextView

        constructor(
            temperature: String,
            realFeel: String,
            addressDescription: String,
            iconName: String,
            hour: String,
            position: Int,
            minutes: String,
            imageViewMainIcon: ImageView,
            mainAct: MainActivity
        ) {
            this.temperature = temperature
            this.realFeel = realFeel
            this.addressDescription = addressDescription
            this.iconName = iconName
            this.hour = hour
            this.position = position
            this.minutes = minutes
            this.imageViewMainIcon = imageViewMainIcon

            this.imageViewMainIcon.setOnClickListener { view ->
                mainAct.updateValues(this)
            }
            this.imageViewMainIcon.isClickable = true
        }
    }

    /** This class sends the hourly information to recyclerview.
     *
     */
    class CustomAdapter(
        private val context: Context,
        private val temperatures: java.util.ArrayList<String>,
        private val realFeels: java.util.ArrayList<String>,
        private val addressDescriptions: java.util.ArrayList<String>,
        private val iconNames: java.util.ArrayList<String>,
        private val hours: java.util.ArrayList<String>,
        private val minutes: java.util.ArrayList<String>,
        private val mainAct: MainActivity
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        lateinit var time: TextView
        lateinit var temp: TextView
        lateinit var icon: ImageView

        var listHourlyWeather = arrayListOf<HourlyWeather>()

        private inner class ViewHolder internal constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {

            init {
                time = itemView.findViewById(R.id.time)
                temp = itemView.findViewById(R.id.temp)
                icon = itemView.findViewById(R.id.icon)
            }

            internal fun bind(position: Int) {

                listHourlyWeather.add(
                    HourlyWeather(
                        temperatures[position],
                        realFeels[position],
                        addressDescriptions[position],
                        iconNames[position],
                        hours[position],
                        position,
                        minutes[position], icon, mainAct
                    )
                )


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

