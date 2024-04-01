package com.weatherapp.fast
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_data", primaryKeys = ["date", "latitude", "longitude"])
data class WeatherEntry(
    val date: String,
    val latitude: Double,
    val longitude: Double,
    val maxTemperature: Double,
    val minTemperature: Double
)