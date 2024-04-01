package com.weatherapp.fast

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
import java.time.LocalDate.parse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = WeatherViewModel()

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
                date = "$year-$date"
                try {
                    viewModel.fetchWeatherData(date,latitude,longitude)
                } catch (e: Exception) {
                    viewModel._error = mutableStateOf("An error occurred. Please try again later.")
                }
                date = ""
                year = ""
                latitude = ""
                longitude = ""
            }
        ) {
            Text("Get Weather")
        }
        Spacer(modifier = Modifier.padding(8.dp))

        var weatherData = viewModel.weatherData
        val error = viewModel.error

        if (error != null) {
            Text(error, color = Color.Red)
        } else {
            weatherData?.let { weather ->
                if (weather.daily?.temperature_2m_max?.isNotEmpty() == true &&
                    weather.daily?.temperature_2m_min?.isNotEmpty() == true) {
                    val maxtemp = getMaxTemperature(weather)
                    val mintemp = getMinTemperature(weather)
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top) {
                            Text("Max Temp: ${maxtemp}°C", style = MaterialTheme.typography.bodyLarge.copy(color = Color.Magenta))
                            Spacer(modifier = Modifier.height(8.dp)) // Smaller spacer for tighter spacing
                            Text("Min Temp: ${mintemp}°C", style = MaterialTheme.typography.bodyLarge.copy(color=Color.DarkGray))
                        }
                    }
                } else {
                    Text("Error occurred, temperature data not available for the specified date")
                }
            }

        }
    }
}

fun getMaxTemperature(weatherData: WeatherData): String {
    return try{
        weatherData.daily.temperature_2m_max[0].toString()
    }
    catch (e: Exception){
        "Error: Not Available for this date"
    }
}
fun getMinTemperature(weatherData: WeatherData): String {
    return try{
        weatherData.daily.temperature_2m_min[0].toString()
    }
    catch (e: Exception){
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

class WeatherViewModel : ViewModel() {
    private val _weatherData = mutableStateOf<WeatherData?>(null)
    val weatherData: WeatherData? get() = _weatherData.value
    var _error = mutableStateOf<String?>(null)
    val error: String? get() = _error.value

    fun fetchWeatherData(date: String,latitude: String,longitude: String) {
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
        if(requestedDate.isAfter(today)){
            _error.value = "Invalid date. Please enter a date in the past or today."
            return
        }
        if (requestedDate.isBefore(tenDaysAgo) ) {
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
                        if (body != null && body.daily?.temperature_2m_max?.isNotEmpty() == true
                            && body.daily?.temperature_2m_min?.isNotEmpty() == true) {
                            _weatherData.value = body
                            _error.value = null
                        } else {
                            _error.value = "Error occurred, temperature data not available for the specified date"
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
        else{
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
                        if (body != null && body.daily?.temperature_2m_max?.isNotEmpty() == true
                            && body.daily?.temperature_2m_min?.isNotEmpty() == true) {
                            _weatherData.value = body
                            _error.value = null
                        } else {
                            _error.value = "Error occurred, temperature data not available for the specified date"
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
fun isValidDate(date: String): Boolean {
    return try {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        parse(date, formatter)
        true
    } catch (e: Exception) {
        false
    }
}
