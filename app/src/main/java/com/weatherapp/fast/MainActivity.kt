package com.weatherapp.fast

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.weatherapp.fast.ui.theme.WeatherAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate.parse


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = WeatherViewModel(application = application)

        setContent {
            WeatherAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun WeatherApp(viewModel: WeatherViewModel) {
    var date by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var maxtemp by remember { mutableStateOf("") }
    var mintemp by remember { mutableStateOf("") }
    var weatherData = viewModel.weatherData
    var error = viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Weather App",
            style = MaterialTheme.typography.headlineMedium.copy(color = Color.Blue),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        TextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (MM-DD)") }
        )
        TextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("Year") }
        )
        TextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text(text = "Latitude") }
        )
        TextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text(text = "Longitude") }
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = {
                error= null
                date = "$year-$date"
                try {

                    CoroutineScope(Dispatchers.IO).launch { // Launch a coroutine
                        val weatherEntry = viewModel.getWeatherFromDatabase(
                            date,
                            latitude.toDouble(),
                            longitude.toDouble()
                        )
                        weatherEntry?.let { weatherEntry ->  // Access weatherEntry directly
                            maxtemp = weatherEntry.maxTemperature.toString()
                            mintemp = weatherEntry.minTemperature.toString()
                            error = null
                            // Display weather data
                            date = ""
                            year = ""
                            latitude = ""
                            longitude = ""
                        }
                            ?: run {
                                // Weather not in database, fetch from API
                                viewModel.fetchWeatherData(date, latitude, longitude)
                                weatherData?.let { weather ->
                                    if (weather.daily.temperature_2m_max?.isNotEmpty() == true &&
                                        weather.daily.temperature_2m_min?.isNotEmpty() == true
                                    ) {
                                        maxtemp = getMaxTemperature(weather)
                                        mintemp = getMinTemperature(weather)

                                    } else {
                                        error =
                                            "Error occurred, temperature data not available for the specified date"
                                    }
                                }
                                date = ""
                                year = ""
                                latitude = ""
                                longitude = ""
                            }
                    }
                } catch (e: Exception) {
                    viewModel._error = mutableStateOf("An error occurred. Please try again later.")
                }

            }
        ) {
            Text("Get Weather")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = {
                error = viewModel.fetchAndStoreHistoricalData(latitude, longitude)
            }
        ) {
            Text("Fetch and Store Historical Data")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        weatherData=viewModel.weatherData
        if(weatherData != null) {
            maxtemp= getMaxTemperature(weatherData!!)
            mintemp = getMinTemperature(weatherData!!)
            error= null
        }

        if (error != null) {
            Text(error!!, color = Color.Red)
        } else {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        "Max Temp: ${maxtemp}°C",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.Magenta)
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Smaller spacer for tighter spacing
                    Text(
                        "Min Temp: ${mintemp}°C",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.DarkGray)
                    )
                }
            }

        }

    }
}

fun getMaxTemperature(weatherData: WeatherData): String {
    return try {
        weatherData.daily.temperature_2m_max[0].toString()
    } catch (e: Exception) {
        "Error: Not Available for this date"
    }
}

fun getMinTemperature(weatherData: WeatherData): String {
    return try {
        weatherData.daily.temperature_2m_min[0].toString()
    } catch (e: Exception) {
        "Error: Not Available for this date"
    }
}

data class WeatherData(
    val latitude: Double,
    val longitude: Double,
    val generationTimeMs: Double,
    val utcOffsetSeconds: Int,
    val timezone: String,
    val timezone_abbreviation: String?,
    val elevation: Int,
    val daily_units: DailyUnits?,
    val daily: Daily
)

data class DailyUnits(
    val time: String,
    val temperature_2m_max: String,
    val temperature_2m_min: String
)

data class Daily(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>
)

class WeatherViewModel(private val application: Application) : ViewModel() {
    private val _weatherData = mutableStateOf<WeatherData?>(null)


    private val database: WeatherDatabase? = WeatherDatabase.getDatabase(application)
    val weatherDao = database?.weatherDao()
    val weatherData: WeatherData? get() = _weatherData.value
    var _error = mutableStateOf<String?>(null)
    val error: String? get() = _error.value


    suspend fun getWeatherFromDatabase(
        date: String,
        latitude: Double,
        longitude: Double
    ): WeatherEntry? {
        if (!isValidDate(date)) {
            _error.value = "Invalid input. Please enter a valid date (MM-DD) and year."
            return null
        }
        val error = validateCoordinates(latitude.toString(), longitude.toString())
        if (error != null) {
            _error.value = error
            return null
        }
        return weatherDao?.getWeatherByDate(
            date,
            roundToTwoDecimals(latitude),
            roundToTwoDecimals(longitude)
        )
    }

    fun fetchWeatherData(date: String, latitude: String, longitude: String) {
        // Validate user input
        if (!isValidDate(date)) {
            _error.value = "Invalid input. Please enter a valid date (MM-DD) and year."
            return
        }
        val error = validateCoordinates(latitude, longitude)
        if (error != null) {
            _error.value = error
            return
        }
        val today = LocalDate.now()
        val tenDaysAgo = today.minusDays(10)
        val requestedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        if (requestedDate.isAfter(today)) {
            CoroutineScope(Dispatchers.IO).launch {
                val avgTemps = calculateAndDisplayAverageTemperatures(
                    requestedDate,
                    latitude.toDouble(),
                    longitude.toDouble()
                )
                if (avgTemps != null) {
                    _error.value = null
                    _weatherData.value = WeatherData(
                        latitude = latitude.toDouble(),
                        longitude = longitude.toDouble(),
                        generationTimeMs = 0.0,
                        utcOffsetSeconds = 0,
                        timezone = "",
                        timezone_abbreviation = "",
                        elevation = 0,
                        daily_units = DailyUnits("", "", ""),
                        daily = Daily(
                            listOf(date),
                            listOf(avgTemps.first),
                            listOf(avgTemps.second)
                        )
                    )
                    _error.value = null
                } else {
                    _error.value = "Error fetching weather data. Please try again later."

                }

            }

            return
        }
        if (requestedDate.isBefore(tenDaysAgo)) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://archive-api.open-meteo.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)
            val call = service.getWeatherData(
                latitude = latitude.toDouble(),
                longitude = longitude.toDouble(),
                startDate = date,
                endDate = date,
                daily = "temperature_2m_max,temperature_2m_min"
            )

            call.enqueue(object : Callback<WeatherData> {
                override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.daily.temperature_2m_max.isNotEmpty() && body.daily.temperature_2m_min?.isNotEmpty() == true) {
                            _weatherData.value = body
                            _error.value = null


                            val weatherEntry = WeatherEntry(
                                date = date,
                                latitude = roundToTwoDecimals(latitude.toDouble()), // Round here
                                longitude = roundToTwoDecimals(longitude.toDouble()),
                                maxTemperature = body.daily.temperature_2m_max[0],
                                minTemperature = body.daily.temperature_2m_min[0]
                            )
                            viewModelScope.launch(Dispatchers.IO) {

                                weatherDao?.insertWeatherData(weatherEntry)
                            }

                        } else {
                            _error.value =
                                "Error occurred, temperature data not available for the specified date"
                        }
                    } else {
                        _error.value = "Error fetching weather data. Please try again later."
                    }
                }

                override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                    _error.value = "Network error. Please check your internet connection."
                }
            })
        } else {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ForecastService::class.java)
            val call = service.getWeatherData(
                latitude = latitude.toDouble(), // Replace with your desired latitude
                longitude = longitude.toDouble(), // Replace with your desired longitude
                startDate = date,
                endDate = date,
                daily = "temperature_2m_max,temperature_2m_min"
            )

            call.enqueue(object : Callback<WeatherData> {
                override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.daily.temperature_2m_max.isNotEmpty() && body.daily.temperature_2m_min.isNotEmpty()) {
                            _weatherData.value = body
                            _error.value = null
                            val weatherEntry = WeatherEntry(
                                date = date,
                                latitude = roundToTwoDecimals(latitude.toDouble()),
                                longitude = roundToTwoDecimals(longitude.toDouble()),
                                maxTemperature = body.daily.temperature_2m_max[0],
                                minTemperature = body.daily.temperature_2m_min[0]
                            )
                            viewModelScope.launch(Dispatchers.IO) {

                                weatherDao?.insertWeatherData(weatherEntry)
                            }

                        } else {
                            _error.value =
                                "Error occurred, temperature data not available for the specified date"
                        }
                    } else {
                        _error.value = "Error fetching weather data. Please try again later."
                    }
                }

                override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                    _error.value = "Network error. Please check your internet connection."
                }
            })
        }
    }

    private suspend fun calculateAndDisplayAverageTemperatures(
        date: LocalDate,
        latitude: Double,
        longitude: Double
    ): Pair<Double, Double>? {
        val historicalDates = getHistoricalDates(date) // Helper to get dates for the last 10 years

        var totalMaxTemp = 0.0
        var totalMinTemp = 0.0
        var count = 0

        for (historicalDate in historicalDates) {
            val weatherEntry = getWeatherFromDatabase(
                historicalDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                latitude,
                longitude
            )

            weatherEntry?.let {
                totalMaxTemp += it.maxTemperature
                totalMinTemp += it.minTemperature
                count++
            }
        }

        return if (count > 0) {
            val avgMaxTemp = totalMaxTemp / count
            val avgMinTemp = totalMinTemp / count
            Pair(roundToTwoDecimals(avgMaxTemp), roundToTwoDecimals(avgMinTemp))
        } else {
            _error.value = "No historical data available to calculate averages."
            null
        }
    }

    private fun getHistoricalDates(date: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val currentYear = LocalDate.now().year
        for (year in currentYear - 10 until currentYear) {
            dates.add(LocalDate.of(year, date.month, date.dayOfMonth))
        }
        return dates
    }

    fun fetchAndStoreHistoricalData(latitude: String, longitude: String): String {
        _error.value= null
        val error = validateCoordinates(latitude, longitude)
        if (error != null) {
            _error.value = error
            return "Invalid coordinates. Please enter valid latitude and longitude."
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = LocalDate.now().minusDays(10)
                val tenYearsAgo = today.minusYears(10)
                val bulkWeatherData = fetchHistoricalDataRange(
                    latitude,
                    longitude,
                    tenYearsAgo.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )

                bulkWeatherData?.daily?.let { dailyData ->
                    for (day in dailyData.time.indices) {
                        val date = dailyData.time[day]
                        val maxTemp = dailyData.temperature_2m_max[day]
                        val minTemp = dailyData.temperature_2m_min[day]

                        val weatherEntry = WeatherEntry(
                            date,
                            latitude.toDouble(),
                            longitude.toDouble(),
                            maxTemp,
                            minTemp
                        )
                        weatherDao?.insertWeatherData(weatherEntry)
                    }
                }

            } catch (e: Exception) {
                _error.value = "Error fetching historical data. Please try again."
            }
        }
        return "Historical data fetched and stored successfully."
    }

    private suspend fun fetchHistoricalDataRange(
        latitude: String,
        longitude: String,
        startDate: String,
        endDate: String
    ): WeatherData? {
        // ... Your API call logic for fetching a date range
        val retrofit = Retrofit.Builder()
            .baseUrl("https://archive-api.open-meteo.com/v1/") // Or modify for your bulk endpoint
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)
        val call = service.getWeatherData( // Assuming a getWeatherDataRange endpoint
            latitude = latitude.toDouble(),
            longitude = longitude.toDouble(),
            startDate = startDate,
            endDate = endDate,
            daily = "temperature_2m_max,temperature_2m_min"
        )

        val response = call.execute() // Synchronous for simplicity
        if (response.isSuccessful) {
            return response.body()
        } else {
            return null // Handle API errors appropriately
        }
    }
}


interface WeatherService {
    @GET("archive")
    fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String
    ): Call<WeatherData>
}

interface ForecastService {
    @GET("forecast")
    fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String,
    ): Call<WeatherData>
}

fun validateCoordinates(latitude: String, longitude: String): String? {
    val latitudeDouble = latitude.toDoubleOrNull()
    val longitudeDouble = longitude.toDoubleOrNull()

    if (latitudeDouble == null) {
        return "Invalid latitude format"
    }
    if (longitudeDouble == null) {
        return "Invalid longitude format"
    }
    if (latitudeDouble !in -90.0..90.0) {
        return "Latitude must be within -90 to 90 degrees"
    }
    if (longitudeDouble !in -180.0..180.0) {
        return "Longitude must be within -180 to 180 degrees"
    }

    return null
}

fun roundToTwoDecimals(value: Double): Double {
    return String.format("%.2f", value).toDouble() // Efficient rounding
}

fun isValidDate(date: String): Boolean {
    return try {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        parse(date, formatter)
        true
    } catch (e: Exception) {
        false
    }
}
